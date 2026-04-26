package com.parkease.booking_service.service.Impl;

import com.parkease.booking_service.client.ParkingSpotServiceClient;
import com.parkease.booking_service.client.ParkingLotServiceClient;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.dtos.ParkingSpotLookupResponseDto;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.entity.BookingStatus;
import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.exception.BookingNotFoundException;
import com.parkease.booking_service.mapper.Impl.BookingRequestMapper;
import com.parkease.booking_service.mapper.Impl.BookingResponseMapper;
import com.parkease.booking_service.repository.BookingRepository;
import com.parkease.booking_service.service.BookingService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("50.00");

    private final BookingRepository bookingRepository;
    private final BookingRequestMapper bookingRequestMapper;
    private final BookingResponseMapper bookingResponseMapper;
    private final ParkingSpotServiceClient parkingSpotServiceClient;
    private final ParkingLotServiceClient parkingLotServiceClient;

    @Override
    public BookingResponseDto createBooking(BookingRequestDto requestDto) {
        log.info("Starting booking creation for userId={}, spotId={}, lotId={}", requestDto.getUserId(), requestDto.getSpotId(), requestDto.getLotId());
        bookingRepository.findActiveBySpotId(requestDto.getSpotId()).ifPresent(existing -> {
            log.error("Booking creation failed. Active booking exists for spotId={}", requestDto.getSpotId());
            throw new IllegalStateException("An active or reserved booking already exists for spot id: "
                    + requestDto.getSpotId());
        });

        ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(requestDto.getSpotId());
        if (!spot.getLotId().equals(requestDto.getLotId())) {
            throw new IllegalStateException("Spot does not belong to the provided lot");
        }
        if (!"AVAILABLE".equalsIgnoreCase(spot.getStatus())) {
            throw new IllegalStateException("Spot is not available for booking");
        }

        reserveSpotOrThrow(requestDto.getSpotId());
        decrementLotAvailabilityOrThrow(requestDto.getLotId());

        Booking booking = bookingRequestMapper.mapFrom(requestDto);
        booking.setStatus(BookingStatus.RESERVED);
        if (booking.getBookingType() == null) {
            booking.setBookingType(BookingType.WALK_IN);
        }

        try {
            Booking savedBooking = bookingRepository.save(booking);
            log.info("Booking created successfully. bookingId={}, status={}", savedBooking.getBookingId(), savedBooking.getStatus());
            return bookingResponseMapper.mapTo(savedBooking);
        } catch (RuntimeException exception) {
            log.error("Error creating booking. Releasing spot and incrementing lot availability for spotId={}, lotId={}", requestDto.getSpotId(), requestDto.getLotId(), exception);
            releaseSpotOrThrow(requestDto.getSpotId());
            incrementLotAvailabilityOrThrow(requestDto.getLotId());
            throw exception;
        }
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);
        return bookingResponseMapper.mapTo(booking);
    }

    @Override
    public List<BookingResponseDto> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotId(lotId)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getActiveBookings() {
        return bookingRepository.findByStatus(BookingStatus.ACTIVE)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    public BookingResponseDto cancelBooking(Long bookingId) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getEndTime() == null) {
            booking.setEndTime(LocalDateTime.now());
        }

        releaseSpotOrThrow(booking.getSpotId());

        Booking updated = bookingRepository.save(booking);
        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    public BookingResponseDto checkIn(Long bookingId) {
        log.info("Starting check-in for bookingId={}", bookingId);
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            log.warn("Check-in failed. Booking is not in RESERVED status. bookingId={}, currentStatus={}", bookingId, booking.getStatus());
            throw new IllegalStateException("Only reserved bookings can be checked in");
        }

        occupySpotOrThrow(booking.getSpotId());
        booking.setStatus(BookingStatus.ACTIVE);
        Booking updated = bookingRepository.save(booking);
        log.info("Check-in successful. bookingId={}", bookingId);
        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    public BookingResponseDto checkOut(Long bookingId, BigDecimal hourlyRate) {
        log.info("Starting check-out for bookingId={}", bookingId);
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            log.warn("Check-out failed. Booking is not in ACTIVE status. bookingId={}, currentStatus={}", bookingId, booking.getStatus());
            throw new IllegalStateException("Only active bookings can be checked out");
        }

        LocalDateTime checkoutTime = LocalDateTime.now();
        booking.setEndTime(checkoutTime);

        BigDecimal finalHourlyRate = hourlyRate == null ? DEFAULT_HOURLY_RATE : hourlyRate;
        BigDecimal totalAmount = calculateAmount(booking.getStartTime(), checkoutTime, finalHourlyRate);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.COMPLETED);

        releaseSpotOrThrow(booking.getSpotId());

        Booking updated = bookingRepository.save(booking);
        log.info("Check-out successful. bookingId={}, totalAmount={}", bookingId, totalAmount);
        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    public BookingResponseDto extendBooking(Long bookingId, LocalDateTime newEndTime) {
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Completed or cancelled bookings cannot be extended");
        }

        if (newEndTime == null) {
            throw new IllegalStateException("New end time is required");
        }

        LocalDateTime baseEndTime = booking.getEndTime() == null ? booking.getStartTime() : booking.getEndTime();
        if (!newEndTime.isAfter(baseEndTime)) {
            throw new IllegalStateException("New end time must be after current booking end time");
        }

        booking.setEndTime(newEndTime);
        Booking updated = bookingRepository.save(booking);
        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    public BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, BigDecimal hourlyRate) {
        if (startTime == null || endTime == null) {
            throw new IllegalStateException("Start time and end time are required");
        }

        if (hourlyRate == null) {
            throw new IllegalStateException("Hourly rate is required");
        }

        if (hourlyRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Hourly rate cannot be negative");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time");
        }

        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);

        return totalHours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<BookingResponseDto> getBookingHistory(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId
                ));
    }

    private ParkingSpotLookupResponseDto fetchSpotOrThrow(Long spotId) {
        try {
            return parkingSpotServiceClient.getSpotById(spotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking spot not found with id: " + spotId);
            }
            throwMappedSpotServiceException(exception,
                    "Unable to validate parking spot with parkingspot-service");
        }
        throw new IllegalStateException("Unexpected execution path while validating parking spot");
    }

    private void occupySpotOrThrow(Long spotId) {
        try {
            parkingSpotServiceClient.occupySpot(spotId);
        } catch (FeignException exception) {
            if (exception.status() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Spot is not available for booking: " + spotId);
            }
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking spot not found with id: " + spotId);
            }
            throwMappedSpotServiceException(exception,
                    "Unable to mark spot as occupied in parkingspot-service");
        }
    }

    private void reserveSpotOrThrow(Long spotId) {
        try {
            parkingSpotServiceClient.reserveSpot(spotId);
        } catch (FeignException exception) {
            if (exception.status() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Spot is not available for booking: " + spotId);
            }
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking spot not found with id: " + spotId);
            }
            throwMappedSpotServiceException(exception,
                    "Unable to reserve spot in parkingspot-service");
        }
    }

    private void releaseSpotOrThrow(Long spotId) {
        try {
            parkingSpotServiceClient.releaseSpot(spotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking spot not found with id: " + spotId);
            }
            if (exception.status() == 409) {
                return;
            }
            throwMappedSpotServiceException(exception,
                    "Unable to release spot in parkingspot-service");
        }
    }

    private void decrementLotAvailabilityOrThrow(Long lotId) {
        try {
            parkingLotServiceClient.decrementAvailable(lotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking lot not found with id: " + lotId);
            }
            if (exception.status() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No available capacity in lot: " + lotId);
            }
            throwMappedLotServiceException(exception,
                    "Unable to decrement lot availability in parkinglot-service");
        }
    }

    private void incrementLotAvailabilityOrThrow(Long lotId) {
        try {
            parkingLotServiceClient.incrementAvailable(lotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking lot not found with id: " + lotId);
            }
            throwMappedLotServiceException(exception,
                    "Unable to increment lot availability in parkinglot-service");
        }
    }

    private void throwMappedSpotServiceException(FeignException exception, String badGatewayMessage) {
        HttpStatus status = HttpStatus.resolve(exception.status());

        if (exception.status() == -1 || status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    badGatewayMessage + " (parkingspot-service unreachable)");
        }

        if (status != null && status.is4xxClientError()) {
            throw new ResponseStatusException(status, "parkingspot-service rejected the request");
        }

        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "parkingspot-service is unavailable");
        }

        if (status.is5xxServerError()) {
            throw new ResponseStatusException(status,
                    "parkingspot-service failed to process the request");
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, badGatewayMessage);
    }

    private void throwMappedLotServiceException(FeignException exception, String badGatewayMessage) {
        HttpStatus status = HttpStatus.resolve(exception.status());

        if (exception.status() == -1 || status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    badGatewayMessage + " (parkinglot-service unreachable)");
        }

        if (status.is4xxClientError()) {
            throw new ResponseStatusException(status, "parkinglot-service rejected the request");
        }

        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "parkinglot-service is unavailable");
        }

        if (status.is5xxServerError()) {
            throw new ResponseStatusException(status,
                    "parkinglot-service failed to process the request");
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, badGatewayMessage);
    }
}