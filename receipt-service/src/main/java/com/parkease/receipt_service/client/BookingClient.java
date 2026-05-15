package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.BookingResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${booking-service.url:http://localhost:8085}/api/internal/bookings")
public interface BookingClient {

    @GetMapping("/{bookingId}")
    BookingResponseDto getBooking(@PathVariable("bookingId") Long bookingId);
}
