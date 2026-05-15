package com.parkease.booking_service.controller;

import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBookingInternal(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                bookingService.getBookingById(bookingId)
        );
    }
}