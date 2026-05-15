package com.parkease.booking_service.service.Impl;

import com.parkease.booking_service.client.ParkingSpotServiceClient;
import com.parkease.booking_service.client.ParkingLotServiceClient;
import com.parkease.booking_service.client.PaymentServiceClient;
import com.parkease.booking_service.client.ReceiptServiceClient;
import com.parkease.booking_service.client.VehicleServiceClient;
import com.parkease.booking_service.dtos.BookingEstimateRequestDto;
import com.parkease.booking_service.dtos.BookingEstimateResponseDto;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.dtos.ParkingLotLookupResponseDto;
import com.parkease.booking_service.dtos.ParkingSpotLookupResponseDto;
import com.parkease.booking_service.dtos.PaymentRequestDto;
import com.parkease.booking_service.dtos.PaymentResponseDto;
import com.parkease.booking_service.dtos.ReceiptResponseDto;
import com.parkease.booking_service.dtos.VehicleLookupResponseDto;
import com.parkease.booking_service.entity.*;

import com.parkease.booking_service.event.NotificationEventPublisher;
import com.parkease.booking_service.exception.BookingNotFoundException;
import com.parkease.booking_service.exception.BookingConflictException;
import com.parkease.booking_service.exception.GracePeriodExpiredException;
import com.parkease.booking_service.exception.InvalidBookingStateException;
import com.parkease.booking_service.exception.PaymentFailedException;
import com.parkease.booking_service.exception.RefundNotAllowedException;
import com.parkease.booking_service.exception.SpotUnavailableException;
import com.parkease.booking_service.exception.UnauthorizedVehicleException;
import com.parkease.booking_service.mapper.Impl.BookingRequestMapper;
import com.parkease.booking_service.mapper.Impl.BookingResponseMapper;
import com.parkease.booking_service.repository.BookingRepository;
import com.parkease.booking_service.service.BookingService;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RequestBody;
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
    private static final long CHECK_IN_GRACE_MINUTES = 15;
    private static final BigDecimal PRE_BOOKING_DEPOSIT_RATE = new BigDecimal("0.20");

    private final BookingRepository bookingRepository;
    private final BookingRequestMapper bookingRequestMapper;
    private final BookingResponseMapper bookingResponseMapper;
    private final ParkingSpotServiceClient parkingSpotServiceClient;
    private final ParkingLotServiceClient parkingLotServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final ReceiptServiceClient receiptServiceClient;
    private final VehicleServiceClient vehicleServiceClient;
    private final NotificationEventPublisher notificationPublisher;

    @Override
    @Transactional
    public BookingResponseDto createBooking(@RequestBody @Valid BookingRequestDto requestDto) {
        log.info("[BOOKING-CREATE] Starting booking creation. userId={}, spotId={}, lotId={}, type={}",
                requestDto.getUserId(), requestDto.getSpotId(), requestDto.getLotId(), requestDto.getBookingType());

        // WALK_IN_BOOKING: auto-set startTime to now
        if (requestDto.getBookingType() == BookingType.WALK_IN_BOOKING) {
            requestDto.setStartTime(LocalDateTime.now());
            log.info("[BOOKING-CREATE] WALK_IN_BOOKING detected — auto-setting startTime={}", requestDto.getStartTime());
        }

        validateRequestedWindow(requestDto);
        validateVehicleOwnership(requestDto);

        ParkingLotLookupResponseDto lot = fetchLotOrThrow(requestDto.getLotId());
        validateLotOpen(lot);

        ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(requestDto.getSpotId());
        if (!spot.getLotId().equals(requestDto.getLotId())) {
            throw new SpotUnavailableException("Spot does not belong to the provided lot");
        }
        if (!"AVAILABLE".equalsIgnoreCase(spot.getStatus())) {
            throw new SpotUnavailableException("Spot is not available for booking");
        }
        validateSpotSupportsVehicle(spot, requestDto.getVehicleType());

        if (bookingRepository.existsOverlappingBooking(
                requestDto.getSpotId(),
                requestDto.getStartTime(),
                requestDto.getEndTime())) {
            throw new BookingConflictException("Requested time window overlaps an existing booking for this spot");
        }

        // Reserve spot atomically — no payment, no receipt
        reserveSpotOrThrow(requestDto.getSpotId());
        decrementLotAvailabilityOrThrow(requestDto.getLotId());

        Booking booking = bookingRequestMapper.mapFrom(requestDto);
        booking.setStatus(BookingStatus.RESERVED);
        booking.setPaid(false);

        BigDecimal hourlyRate = spot.getPricePerHour() != null
                ? BigDecimal.valueOf(spot.getPricePerHour())
                : DEFAULT_HOURLY_RATE;
        BigDecimal dailyRate = hourlyRate.multiply(BigDecimal.valueOf(10));

        BigDecimal estimatedAmount = calculateAmount(
                booking.getStartTime(), booking.getEndTime(),
                booking.getPricingType(), hourlyRate, dailyRate);
        booking.setTotalAmount(estimatedAmount);

        long minutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
        booking.setDuration(formatDuration(minutes));

        try {
            Booking savedBooking = bookingRepository.save(booking);
            log.info("[BOOKING-CREATE] Booking created. bookingId={}, status=RESERVED, estimatedAmount={}",
                    savedBooking.getBookingId(), savedBooking.getTotalAmount());

            // NO payment initialization here — payment happens only during checkout
            notificationPublisher.publishBookingConfirmed(
                    savedBooking.getUserId(),
                    savedBooking.getBookingId(),
                    requestDto.getEmail()
            );

            return bookingResponseMapper.mapTo(savedBooking);

        } catch (RuntimeException exception) {
            log.error("[BOOKING-CREATE] Error — releasing spot. spotId={}, lotId={}",
                    requestDto.getSpotId(), requestDto.getLotId(), exception);
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
    public List<BookingResponseDto> getActiveBookingsByLot(Long lotId) {
List<BookingStatus> status = List.of(
            BookingStatus.RESERVED,
            BookingStatus.ACTIVE
        );
        return bookingRepository.findByLotIdAndStatusIn(lotId, status)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getActiveBookings() {
List<BookingStatus> status = List.of(
            BookingStatus.RESERVED,
            BookingStatus.ACTIVE
        );
        return bookingRepository.findByStatusIn(status)
                .stream()
                .map(bookingResponseMapper::mapTo)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long bookingId) {
        Booking booking = findBookingForUpdateOrThrow(bookingId);

if (!List.of(BookingStatus.RESERVED).contains(booking.getStatus())) {
            if (booking.getStatus() == BookingStatus.ACTIVE) {
                throw new RefundNotAllowedException("Active bookings cannot be cancelled or refunded");
            }
            throw new InvalidBookingStateException("Booking cannot be cancelled in status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);

        releaseSpotOrThrow(booking.getSpotId());
        incrementLotAvailabilityOrThrow(booking.getLotId());
        refundBookingIfEligible(booking, false);

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
        Booking booking = findBookingForUpdateOrThrow(bookingId);

        if (isGracePeriodExpired(booking)) {
            booking.setStatus(BookingStatus.CANCELLED);
            releaseSpotOrThrow(booking.getSpotId());
            incrementLotAvailabilityOrThrow(booking.getLotId());
            refundBookingIfEligible(booking, true);
            bookingRepository.save(booking);
            throw new GracePeriodExpiredException("Booking check-in grace period has expired");
        }

        if (!List.of(BookingStatus.RESERVED).contains(booking.getStatus())) {
            log.warn("Check-in failed. Booking is not in RESERVED status. bookingId={}, currentStatus={}",
                    bookingId, booking.getStatus());
            throw new InvalidBookingStateException("Only reserved bookings can be checked in");
        }

        occupySpotOrThrow(booking.getSpotId());

        booking.setCheckInTime(LocalDateTime.now());
        booking.setStatus(BookingStatus.ACTIVE);
        Booking updated = bookingRepository.save(booking);

        log.info("Check-in successful. bookingId={}", bookingId);

        notificationPublisher.publishCheckIn(
                updated.getUserId(),
                updated.getBookingId(),
                null
        );

        return bookingResponseMapper.mapTo(updated);
    }

    @Transactional
    public BookingResponseDto initiateCheckout(Long bookingId) {
        log.info("[CHECKOUT-INIT] Initiating checkout for bookingId={}", bookingId);
        Booking booking = findBookingForUpdateOrThrow(bookingId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException("Only ACTIVE bookings can initiate checkout");
        }

        LocalDateTime checkoutTime = LocalDateTime.now();
        LocalDateTime effectiveStart = resolveStartTime(booking);

        BigDecimal hourlyRate = DEFAULT_HOURLY_RATE;
        try {
            ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(booking.getSpotId());
            if (spot.getPricePerHour() != null) {
                hourlyRate = BigDecimal.valueOf(spot.getPricePerHour());
            }
        } catch (Exception e) {
            log.warn("[CHECKOUT-INIT] Using default hourly rate for bookingId={}", bookingId);
        }

        BigDecimal dailyRate = hourlyRate.multiply(BigDecimal.valueOf(10));
        BigDecimal finalAmount = calculateAmount(effectiveStart, checkoutTime,
                booking.getPricingType(), hourlyRate, dailyRate);

        booking.setCheckOutTime(checkoutTime);
        booking.setTotalAmount(finalAmount);
        long minutes = Duration.between(effectiveStart, checkoutTime).toMinutes();
        booking.setDuration(formatDuration(minutes));

        Booking saved = bookingRepository.save(booking);
        log.info("[CHECKOUT-INIT] Amount stored. bookingId={}, finalAmount={}", bookingId, finalAmount);

        return bookingResponseMapper.mapTo(saved);
    }

    @Override
    @Transactional
    public BookingResponseDto checkOut(Long bookingId, BigDecimal hourlyRate) {
        // Now checkOut just initiates and returns the estimate
        return initiateCheckout(bookingId);
    }
    @Override
    @Transactional
    public BookingResponseDto markAsPaid(Long bookingId) {
        log.info("[MARK-PAID] Payment verified for bookingId={} — completing booking", bookingId);

        Booking booking = findBookingForUpdateOrThrow(bookingId);

        // Payment verified during checkout → booking must be ACTIVE
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            log.warn("[MARK-PAID] Booking not in ACTIVE state. bookingId={}, status={}",
                    bookingId, booking.getStatus());
            // If it was already COMPLETED (idempotent), return as-is
            if (booking.getStatus() == BookingStatus.COMPLETED) {
                return bookingResponseMapper.mapTo(booking);
            }
            throw new InvalidBookingStateException(
                    "Payment verification failed: booking must be ACTIVE to complete. Current: " + booking.getStatus());
        }

        // Finalize state
        booking.setPaid(true);
        booking.setStatus(BookingStatus.COMPLETED);

        // Ensure we have a checkout time if not already set by initiateCheckout
        if (booking.getCheckOutTime() == null) {
            booking.setCheckOutTime(LocalDateTime.now());
        }

        releaseSpotOrThrow(booking.getSpotId());
        incrementLotAvailabilityOrThrow(booking.getLotId());

        Booking saved = bookingRepository.saveAndFlush(booking);
        log.info("[MARK-PAID] Booking completed. bookingId={}, finalAmount={}",
                saved.getBookingId(), saved.getTotalAmount());

        notificationPublisher.publishCheckOut(
                saved.getUserId(), saved.getBookingId(), null,
                saved.getTotalAmount().toPlainString());

        return bookingResponseMapper.mapTo(saved);
    }


    @Override
    @Transactional
    public BookingResponseDto extendBooking(Long bookingId, LocalDateTime newEndTime) {
        Booking booking = findBookingOrThrow(bookingId);

        if (List.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED)
                .contains(booking.getStatus())) {
            throw new InvalidBookingStateException("Completed or cancelled bookings cannot be extended");
        }

        if (newEndTime == null) {
            throw new IllegalStateException("New end time is required");
        }

        LocalDateTime baseEndTime = booking.getEndTime() == null ? booking.getStartTime() : booking.getEndTime();
        if (!newEndTime.isAfter(baseEndTime)) {
            throw new IllegalStateException("New end time must be after current booking end time");
        }

        BigDecimal previousAmount = booking.getTotalAmount();
        booking.setEndTime(newEndTime);

        BigDecimal hourlyRate = DEFAULT_HOURLY_RATE;
        BigDecimal dailyRate = hourlyRate.multiply(BigDecimal.valueOf(10));

        BigDecimal amount = calculateAmount(
            resolveStartTime(booking), booking.getEndTime(),
                booking.getPricingType(), hourlyRate, dailyRate);
        booking.setTotalAmount(amount);

        // If already active/paid, extending may require additional payment.
        // Final-state alignment: keep the booking ACTIVE but mark unpaid to force payment.
        if (booking.isPaid() && previousAmount != null && amount.compareTo(previousAmount) > 0) {
            booking.setPaid(false);
        }


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
            long billableMinutes = Math.max(60, totalMinutes);
            BigDecimal hours = BigDecimal.valueOf(billableMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return hRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
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
        log.info("[ESTIMATE] Estimating booking. lotId={}, spotId={}, type={}",
                requestDto.getLotId(), requestDto.getSpotId(), requestDto.getBookingType());

        LocalDateTime startTime = requestDto.getStartTime();
        if (requestDto.getBookingType() == com.parkease.booking_service.entity.BookingType.WALK_IN_BOOKING || startTime == null) {
            startTime = LocalDateTime.now();
            requestDto.setStartTime(startTime);
            log.info("[ESTIMATE] WALK_IN mode or null startTime — using server 'now': {}", startTime);
        }

        if (!requestDto.getEndTime().isAfter(startTime)) {
            throw new com.parkease.booking_service.exception.InvalidBookingStateException("End time must be after start time");
        }

        ParkingSpotLookupResponseDto spot = fetchSpotOrThrow(requestDto.getSpotId());
        if (!spot.getLotId().equals(requestDto.getLotId())) {
            throw new com.parkease.booking_service.exception.InvalidBookingStateException("Spot does not belong to the provided lot");
        }

        BigDecimal hourlyRate = spot.getPricePerHour() != null
                ? BigDecimal.valueOf(spot.getPricePerHour())
                : DEFAULT_HOURLY_RATE;

        BigDecimal totalAmount = calculateAmount(startTime, requestDto.getEndTime(), hourlyRate);
        long durationMinutes = Duration.between(startTime, requestDto.getEndTime()).toMinutes();

        BookingEstimateResponseDto response = BookingEstimateResponseDto.builder()
                .lotId(requestDto.getLotId())
                .spotId(requestDto.getSpotId())
                .startTime(startTime)
                .endTime(requestDto.getEndTime())
                .totalAmount(totalAmount)
                .hourlyRate(hourlyRate)
                .durationMinutes(durationMinutes)
                .build();

        log.info("[ESTIMATE] Calculated totalAmount={}, duration={} min", totalAmount, durationMinutes);
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

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCancelExpiredReservations() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(CHECK_IN_GRACE_MINUTES);
        List<Booking> expiredBookings = bookingRepository.findExpiredReservedBookings(expiredBefore);

        for (Booking booking : expiredBookings) {
            try {
                Booking locked = findBookingForUpdateOrThrow(booking.getBookingId());
                if (locked.getStatus() != BookingStatus.RESERVED || !isGracePeriodExpired(locked)) {
                    continue;
                }
                locked.setStatus(BookingStatus.CANCELLED);
                releaseSpotOrThrow(locked.getSpotId());
                incrementLotAvailabilityOrThrow(locked.getLotId());
                refundBookingIfEligible(locked, true);
                bookingRepository.save(locked);
                notificationPublisher.publishBookingCancelled(locked.getUserId(), locked.getBookingId(), null);
                log.info("Auto-cancelled expired reservation bookingId={}", locked.getBookingId());
            } catch (RuntimeException exception) {
                log.error("Failed to auto-cancel bookingId={}", booking.getBookingId(), exception);
            }
        }
    }

    private Booking findBookingOrThrow(Long bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId));
    }

    private Booking findBookingForUpdateOrThrow(Long bookingId) {
        return bookingRepository.findByBookingIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with id: " + bookingId));
    }

    private void validateRequestedWindow(BookingRequestDto requestDto) {
        LocalDateTime startTime = requestDto.getStartTime();
        LocalDateTime endTime = requestDto.getEndTime();
        if (endTime == null) {
            throw new InvalidBookingStateException("End time is required");
        }
        if (startTime == null) {
            throw new InvalidBookingStateException("Start time is required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidBookingStateException("End time must be after start time");
        }
        // PRE_BOOKING: startTime must not be in the past (with 5 min grace)
        // WALK_IN_BOOKING: startTime is auto-set to now, so no past check needed
        if (requestDto.getBookingType() == BookingType.PRE_BOOKING
                && startTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new InvalidBookingStateException("PRE_BOOKING start time cannot be in the past");
        }
    }

    // Legacy overload kept for internal usage
    private void validateRequestedWindow(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new InvalidBookingStateException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new InvalidBookingStateException("End time must be after start time");
        }
    }

    private boolean isGracePeriodExpired(Booking booking) {
        return LocalDateTime.now().isAfter(booking.getStartTime().plusMinutes(CHECK_IN_GRACE_MINUTES));
    }

    private void validateLotOpen(ParkingLotLookupResponseDto lot) {
        if (lot.getOpen() == null || !lot.getOpen()) {
            throw new SpotUnavailableException("Parking lot is currently closed");
        }
        if (lot.getApproved() != null && !lot.getApproved()) {
            throw new SpotUnavailableException("Parking lot is not approved for bookings");
        }
        if (lot.getAvailableSpots() != null && lot.getAvailableSpots() <= 0) {
            throw new SpotUnavailableException("Parking lot has no available spots");
        }
    }

    private void validateVehicleOwnership(BookingRequestDto requestDto) {
        try {
            VehicleLookupResponseDto vehicle = vehicleServiceClient.getByLicensePlate(requestDto.getVehiclePlate());
            if (vehicle == null || !requestDto.getUserId().equals(vehicle.getOwnerId())) {
                throw new UnauthorizedVehicleException("Vehicle does not belong to this user");
            }
            Boolean active = vehicle.getActive() != null ? vehicle.getActive() : vehicle.getIsActive();
            if (Boolean.FALSE.equals(active)) {
                throw new UnauthorizedVehicleException("Vehicle is inactive");
            }
            if (vehicle.getVehicleType() != null
                    && !vehicle.getVehicleType().equalsIgnoreCase(requestDto.getVehicleType())) {
                throw new UnauthorizedVehicleException("Vehicle type does not match the registered vehicle");
            }
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new UnauthorizedVehicleException("Vehicle is not registered");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Unable to validate vehicle ownership with vehicle-service");
        }
    }

    private void validateSpotSupportsVehicle(ParkingSpotLookupResponseDto spot, String requestedVehicleType) {
        if (spot.getVehicleType() != null && requestedVehicleType != null
                && !spot.getVehicleType().equalsIgnoreCase(requestedVehicleType)) {
            throw new SpotUnavailableException("Selected spot does not support vehicle type " + requestedVehicleType);
        }
    }

    // Payment initialization at booking creation is REMOVED.
    // Payment happens ONLY during checkout via Razorpay.
    // The following methods are kept as no-ops for backward compat with any remaining callers.

    /** @deprecated Payment no longer initialized at booking creation */
    @Deprecated
    private void schedulePaymentInitialization(Booking booking) {
        log.debug("[DEPRECATED] schedulePaymentInitialization called but skipped. bookingId={}",
                booking.getBookingId());
        // Intentionally empty — payment happens at checkout
    }

    private void refundBookingIfEligible(Booking booking, boolean autoCancel) {
        if (booking.getStatus() == BookingStatus.ACTIVE) {
            throw new RefundNotAllowedException("No refund is allowed after booking check-in");
        }
        try {
            paymentServiceClient.refundByBookingId(booking.getBookingId());
        } catch (FeignException exception) {
            if (exception.status() == 404 || exception.status() == 409) {
                log.info("No refundable payment found for bookingId={}", booking.getBookingId());
                return;
            }
            throwMappedPaymentServiceException(exception,
                    autoCancel ? "Unable to refund auto-cancelled booking" : "Unable to refund cancelled booking");
        }
    }

    private LocalDateTime resolveStartTime(Booking booking) {
        return booking.getCheckInTime() != null ? booking.getCheckInTime() : booking.getStartTime();
    }

    private PaymentResponseDto fetchPaymentOrThrow(Long bookingId) {
        try {
            return paymentServiceClient.getByBookingId(bookingId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                        "Payment is required before checkout");
            }
            throwMappedPaymentServiceException(exception,
                    "Unable to validate payment status with payment-service");
        }
        throw new IllegalStateException("Unexpected execution path while validating payment");
    }

    private void ensureReceiptGenerated(Long paymentId) {
        try {
            ReceiptResponseDto receipt = receiptServiceClient.getByPaymentId(paymentId);
            if (receipt == null || receipt.getReceiptId() == null) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                        "Receipt has not been generated yet");
            }
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                        "Receipt has not been generated yet");
            }
            throwMappedReceiptServiceException(exception,
                    "Unable to validate receipt status with receipt-service");
        }
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

    private ParkingLotLookupResponseDto fetchLotOrThrow(Long lotId) {
        try {
            return parkingLotServiceClient.getLotById(lotId);
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parking lot not found with id: " + lotId);
            }
            throwMappedLotServiceException(exception,
                    "Unable to validate parking lot with parkinglot-service");
        }
        throw new IllegalStateException("Unexpected execution path while validating parking lot");
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

    private void throwMappedPaymentServiceException(FeignException exception, String badGatewayMessage) {
        HttpStatus status = HttpStatus.resolve(exception.status());

        if (exception.status() == -1 || status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    badGatewayMessage + " (payment-service unreachable)");
        }
        if (status.is4xxClientError()) {
            throw new ResponseStatusException(status, "payment-service rejected the request");
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "payment-service is unavailable");
        }
        if (status.is5xxServerError()) {
            throw new ResponseStatusException(status,
                    "payment-service failed to process the request");
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, badGatewayMessage);
    }

    private void throwMappedReceiptServiceException(FeignException exception, String badGatewayMessage) {
        HttpStatus status = HttpStatus.resolve(exception.status());

        if (exception.status() == -1 || status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    badGatewayMessage + " (receipt-service unreachable)");
        }
        if (status.is4xxClientError()) {
            throw new ResponseStatusException(status, "receipt-service rejected the request");
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "receipt-service is unavailable");
        }
        if (status.is5xxServerError()) {
            throw new ResponseStatusException(status,
                    "receipt-service failed to process the request");
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, badGatewayMessage);
    }
}
