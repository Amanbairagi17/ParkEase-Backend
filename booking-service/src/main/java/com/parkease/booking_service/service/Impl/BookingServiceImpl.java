package com.parkease.booking_service.service.Impl;

import com.parkease.booking_service.client.ParkingSpotServiceClient;
import com.parkease.booking_service.client.ParkingLotServiceClient;
import com.parkease.booking_service.dtos.BookingEstimateRequestDto;
import com.parkease.booking_service.dtos.BookingEstimateResponseDto;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.dtos.ParkingSpotLookupResponseDto;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.entity.BookingStatus;
import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.entity.PricingType;
import com.parkease.booking_service.event.NotificationEventPublisher;
import com.parkease.booking_service.exception.BookingNotFoundException;
import com.parkease.booking_service.mapper.Impl.BookingRequestMapper;
import com.parkease.booking_service.mapper.Impl.BookingResponseMapper;
import com.parkease.booking_service.repository.BookingRepository;
import com.parkease.booking_service.service.BookingService;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private static final BigDecimal DEFAULT_DAILY_RATE = new BigDecimal("500.00");

    private final BookingRepository bookingRepository;
    private final BookingRequestMapper bookingRequestMapper;
    private final BookingResponseMapper bookingResponseMapper;
    private final ParkingSpotServiceClient parkingSpotServiceClient;
    private final ParkingLotServiceClient parkingLotServiceClient;
    private final NotificationEventPublisher notificationPublisher;

    @Override
    public BookingResponseDto createBooking(@RequestBody @Valid BookingRequestDto requestDto) {
        log.info("Starting booking creation for userId={}, spotId={}, lotId={}",
                requestDto.getUserId(), requestDto.getSpotId(), requestDto.getLotId());

        if (requestDto.getEndTime() == null || !requestDto.getEndTime().isAfter(requestDto.getStartTime())) {
            throw new IllegalStateException("End time must be after start time");
        }

        bookingRepository.findActiveBySpotId(requestDto.getSpotId()).ifPresent(existing -> {
            log.error("Booking creation failed. Active booking exists for spotId={}",
                    requestDto.getSpotId());
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

        BigDecimal hourlyRate = spot.getPricePerHour() != null
                ? BigDecimal.valueOf(spot.getPricePerHour())
                : DEFAULT_HOURLY_RATE;

        // Daily rate assumed as 10x hourly if not explicitly provided
        BigDecimal dailyRate = hourlyRate.multiply(BigDecimal.valueOf(10));

        BigDecimal amount = calculateAmount(
                booking.getStartTime(), booking.getEndTime(),
                booking.getPricingType(), hourlyRate, dailyRate);
        booking.setTotalAmount(amount);

        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        booking.setDuration(formatDuration(minutes));
        log.info("Calculated amount BEFORE save: {}", amount);
        try {

            Booking savedBooking = bookingRepository.save(booking);
            log.info("Booking created successfully. bookingId={}, status={}, amount={}",
                    savedBooking.getBookingId(), savedBooking.getStatus(), savedBooking.getTotalAmount());

            notificationPublisher.publishBookingConfirmed(
                    savedBooking.getUserId(),
                    savedBooking.getBookingId(),
                    requestDto.getEmail()
            );
            log.info("Calculated amount AFTER save: {}", savedBooking.getTotalAmount());

            return bookingResponseMapper.mapTo(savedBooking);

        } catch (RuntimeException exception) {
            log.error("Error creating booking. Releasing spot and incrementing lot availability " +
                    "for spotId={}, lotId={}", requestDto.getSpotId(), requestDto.getLotId(), exception);
            releaseSpotOrThrow(requestDto.getSpotId());
            incrementLotAvailabilityOrThrow(requestDto.getLotId());
            throw exception;
        }
    }

    private String formatDuration(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", minutes);
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
    @Transactional
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
        incrementLotAvailabilityOrThrow(booking.getLotId());

        Booking updated = bookingRepository.save(booking);

        // FIX B-6: email not stored on Booking, pass null — publisher falls back to APP channel
        notificationPublisher.publishBookingCancelled(
                updated.getUserId(),
                updated.getBookingId(),
                null
        );

        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    @Transactional
    public BookingResponseDto checkIn(Long bookingId) {
        log.info("Starting check-in for bookingId={}", bookingId);
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.RESERVED) {
            log.warn("Check-in failed. Booking is not in RESERVED status. bookingId={}, currentStatus={}",
                    bookingId, booking.getStatus());
            throw new IllegalStateException("Only reserved bookings can be checked in");
        }

        occupySpotOrThrow(booking.getSpotId());
        booking.setStatus(BookingStatus.ACTIVE);
        Booking updated = bookingRepository.save(booking);
        log.info("Check-in successful. bookingId={}", bookingId);

        // FIX B-6: email not stored on Booking, pass null — publisher falls back to APP channel
        notificationPublisher.publishCheckIn(
                updated.getUserId(),
                updated.getBookingId(),
                null
        );

        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    @Transactional
    public BookingResponseDto checkOut(Long bookingId, BigDecimal hourlyRate) {
        log.info("Starting check-out for bookingId={}", bookingId);
        Booking booking = findBookingOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            log.warn("Check-out failed. Booking is not in ACTIVE status. bookingId={}, currentStatus={}",
                    bookingId, booking.getStatus());
            throw new IllegalStateException("Only active bookings can be checked out");
        }

        LocalDateTime checkoutTime = LocalDateTime.now();
        booking.setEndTime(checkoutTime);

        BigDecimal finalHourlyRate = hourlyRate == null ? DEFAULT_HOURLY_RATE : hourlyRate;
        BigDecimal totalAmount = calculateAmount(booking.getStartTime(), checkoutTime, finalHourlyRate);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.COMPLETED);

        releaseSpotOrThrow(booking.getSpotId());
        incrementLotAvailabilityOrThrow(booking.getLotId());

        Booking updated = bookingRepository.save(booking);
        log.info("Check-out successful. bookingId={}, totalAmount={}", bookingId, totalAmount);

        // FIX B-6: email not stored on Booking (pass null); convert BigDecimal to String for publisher
        notificationPublisher.publishCheckOut(
                updated.getUserId(),
                updated.getBookingId(),
                null,
                updated.getTotalAmount().toPlainString()
        );

        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    @Transactional
    public BookingResponseDto markAsPaid(Long bookingId) {
        log.info("Marking booking as paid. bookingId={}", bookingId);
        Booking booking = findBookingOrThrow(bookingId);
        booking.setPaid(true);

        if (booking.getDuration() == null && booking.getStartTime() != null && booking.getEndTime() != null) {
            long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            booking.setDuration(formatDuration(minutes));
        }

        Booking saved = bookingRepository.save(booking);
        return bookingResponseMapper.mapTo(saved);
    }

    @Override
    @Transactional
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

        BigDecimal hourlyRate = DEFAULT_HOURLY_RATE;
        BigDecimal dailyRate = hourlyRate.multiply(BigDecimal.valueOf(10));

        BigDecimal amount = calculateAmount(
                booking.getStartTime(), booking.getEndTime(),
                booking.getPricingType(), hourlyRate, dailyRate);
        booking.setTotalAmount(amount);

        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        booking.setDuration(formatDuration(minutes));

        Booking updated = bookingRepository.save(booking);
        return bookingResponseMapper.mapTo(updated);
    }

    @Override
    public BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime,
                                      PricingType type, BigDecimal hourlyRate, BigDecimal dailyRate) {
        if (startTime == null || endTime == null) {
            throw new IllegalStateException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time");
        }

        BigDecimal hRate = hourlyRate == null ? DEFAULT_HOURLY_RATE : hourlyRate;
        BigDecimal dRate = dailyRate == null ? DEFAULT_DAILY_RATE : dailyRate;

        Duration duration = Duration.between(startTime, endTime);
        long totalMinutes = duration.toMinutes();

        if (type == PricingType.DAILY) {
            long days = (long) Math.ceil(totalMinutes / 1440.0);
            return dRate.multiply(BigDecimal.valueOf(Math.max(1, days))).setScale(2, RoundingMode.HALF_UP);
        } else {
            long hours = (long) Math.ceil(totalMinutes / 60.0);
            return hRate.multiply(BigDecimal.valueOf(Math.max(1, hours))).setScale(2, RoundingMode.HALF_UP);
        }
    }

    @Override
    public BigDecimal calculateAmount(LocalDateTime startTime, LocalDateTime endTime, BigDecimal hourlyRate) {
        return calculateAmount(startTime, endTime, PricingType.HOURLY, hourlyRate, DEFAULT_DAILY_RATE);
    }

    @Override
    public List<BookingResponseDto> getBookingHistory(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    public BookingEstimateResponseDto estimateBooking(BookingEstimateRequestDto requestDto) {
        log.info("Estimating booking. lotId={}, spotId={}, startTime={}, endTime={}",
                requestDto.getLotId(), requestDto.getSpotId(),
                requestDto.getStartTime(), requestDto.getEndTime());

        if (!requestDto.getEndTime().isAfter(requestDto.getStartTime())) {
            throw new IllegalStateException("End time must be after start time");
        }

        ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(requestDto.getSpotId());
        if (!spot.getLotId().equals(requestDto.getLotId())) {
            throw new IllegalStateException("Spot does not belong to the provided lot");
        }

        BigDecimal hourlyRate = spot.getPricePerHour() != null
                ? BigDecimal.valueOf(spot.getPricePerHour())
                : DEFAULT_HOURLY_RATE;

        BigDecimal totalAmount = calculateAmount(
                requestDto.getStartTime(), requestDto.getEndTime(), hourlyRate);
        long durationMinutes = Duration.between(
                requestDto.getStartTime(), requestDto.getEndTime()).toMinutes();

        BookingEstimateResponseDto response = BookingEstimateResponseDto.builder()
                .lotId(requestDto.getLotId())
                .spotId(requestDto.getSpotId())
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .totalAmount(totalAmount)
                .hourlyRate(hourlyRate)
                .durationMinutes(durationMinutes)
                .build();

        log.info("Booking estimate calculated. totalAmount={}, hourlyRate={}", totalAmount, hourlyRate);
        return response;
    }

    @Override
    public BigDecimal getBookingEstimate(Long bookingId) {
        log.info("Calculating estimate for existing booking. bookingId={}", bookingId);

        Booking booking = findBookingOrThrow(bookingId);
        if (booking.getEndTime() == null) {
            throw new IllegalStateException("End time is required for estimate");
        }

        ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(booking.getSpotId());

        BigDecimal hourlyRate = spot.getPricePerHour() != null
                ? BigDecimal.valueOf(spot.getPricePerHour())
                : DEFAULT_HOURLY_RATE;

        BigDecimal estimate = calculateAmount(booking.getStartTime(), booking.getEndTime(), hourlyRate);
        log.info("Estimate calculated for bookingId={}, estimate={}", bookingId, estimate);
        return estimate;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId));
    }

    private ParkingSpotLookupResponseDto fetchSpotOrThrow(Long spotId) {
        try {
            return parkingSpotServiceClient.getSpotById(spotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking spot not found with id: " + spotId);
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
                return; // already released, treat as success
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
        if (status.is4xxClientError()) {
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