package com.parkease.booking_service.controller;

import com.parkease.booking_service.dtos.BookingEstimateRequestDto;
import com.parkease.booking_service.dtos.BookingEstimateResponseDto;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/bookings")
@RequiredArgsConstructor
@Validated
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto requestDto,
                                                            @RequestHeader(value = "X-User-Name", required = false) String email) {
        requestDto.setEmail(email); // inject email from gateway header

        log.info("Request received to create booking. userId={}, spotId={}, lotId={}",
                requestDto.getUserId(), requestDto.getSpotId(), requestDto.getLotId());

        BookingResponseDto response = bookingService.createBooking(requestDto);

        log.info("Booking created successfully. bookingId={}", response.getBookingId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBookingById(@PathVariable Long bookingId) {
        log.info("Fetching booking by id. bookingId={}", bookingId);

        BookingResponseDto response = bookingService.getBookingById(bookingId);

        log.info("Returning booking details. bookingId={}", bookingId);
        return ResponseEntity.ok(response);
    }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByUser(@PathVariable Long userId) {
        log.info("Fetching bookings for user. userId={}", userId);

        List<BookingResponseDto> response = bookingService.getBookingsByUser(userId);

        log.info("Returning {} bookings for userId={}", response.size(), userId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByLot(@PathVariable Long lotId) {
        log.info("Fetching bookings for lot. lotId={}", lotId);

        List<BookingResponseDto> response = bookingService.getBookingsByLot(lotId);

        log.info("Returning {} bookings for lotId={}", response.size(), lotId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @GetMapping("/active")
    public ResponseEntity<List<BookingResponseDto>> getActiveBookings() {
        log.info("Fetching active bookings");

        List<BookingResponseDto> response = bookingService.getActiveBookings();

        log.info("Returning {} active bookings", response.size());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long bookingId) {
        log.info("Cancelling booking. bookingId={}", bookingId);

        BookingResponseDto response = bookingService.cancelBooking(bookingId);

        log.info("Booking cancelled. bookingId={}, status={}", bookingId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @PutMapping("/{bookingId}/checkIn")
    public ResponseEntity<BookingResponseDto> checkIn(@PathVariable Long bookingId) {
        log.info("Check-in request for booking. bookingId={}", bookingId);

        BookingResponseDto response = bookingService.checkIn(bookingId);

        log.info("Check-in completed. bookingId={}, status={}", bookingId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @PutMapping("/{bookingId}/checkOut")
    public ResponseEntity<BookingResponseDto> checkOut(@PathVariable Long bookingId,
                                                       @RequestParam(required = false) BigDecimal hourlyRate) {
        log.info("Check-out request for booking. bookingId={}", bookingId);

        BookingResponseDto response = bookingService.checkOut(bookingId, hourlyRate);

        log.info("Check-out completed. bookingId={}, status={}, totalAmount={}",
                bookingId, response.getStatus(), response.getTotalAmount());

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @PutMapping("/{bookingId}/extend")
    public ResponseEntity<BookingResponseDto> extendBooking(@PathVariable Long bookingId,
                                                            @RequestParam LocalDateTime newEndTime) {
        log.info("Extending booking. bookingId={}, newEndTime={}", bookingId, newEndTime);

        BookingResponseDto response = bookingService.extendBooking(bookingId, newEndTime);

        log.info("Booking extended. bookingId={}, newEndTime={}", bookingId, newEndTime);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/calculateAmount")
    public ResponseEntity<BigDecimal> calculateAmount(@RequestParam LocalDateTime startTime,
                                                      @RequestParam LocalDateTime endTime,
                                                      @RequestParam BigDecimal hourlyRate) {
        log.info("Calculating amount. startTime={}, endTime={}, rate={}", startTime, endTime, hourlyRate);

        BigDecimal response = bookingService.calculateAmount(startTime, endTime, hourlyRate);

        log.info("Calculated amount={}", response);
        return ResponseEntity.ok(response);
    }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingHistory(@PathVariable Long userId) {
        log.info("Fetching booking history for userId={}", userId);

        List<BookingResponseDto> response = bookingService.getBookingHistory(userId);

        log.info("Returning {} historical bookings for userId={}", response.size(), userId);
        return ResponseEntity.ok(response);
    }

@PreAuthorize("isAuthenticated()")
    @PostMapping("/estimate")
    public ResponseEntity<BookingEstimateResponseDto> estimateBooking(@Valid @RequestBody BookingEstimateRequestDto requestDto) {
        log.info("Estimate request. lotId={}, spotId={}, startTime={}, endTime={}",
                requestDto.getLotId(), requestDto.getSpotId(), requestDto.getStartTime(), requestDto.getEndTime());

        BookingEstimateResponseDto response = bookingService.estimateBooking(requestDto);

        log.info("Estimate response. totalAmount={}", response.getTotalAmount());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @bookingSecurity.isOwner(#bookingId)")
    @GetMapping("/{bookingId}/estimate")
    public ResponseEntity<BigDecimal> getBookingEstimate(@PathVariable Long bookingId) {
        log.info("Getting estimate for booking. bookingId={}", bookingId);

        BigDecimal estimate = bookingService.getBookingEstimate(bookingId);

        log.info("Booking estimate calculated. bookingId={}, estimate={}", bookingId, estimate);
        return ResponseEntity.ok(estimate);
    }
}

