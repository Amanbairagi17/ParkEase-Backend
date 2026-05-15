package com.parkease.payment_service.client;

import com.parkease.payment_service.dtos.BookingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "BOOKING-SERVICE", path = "/api/bookings")
public interface BookingClient {

    @GetMapping("/{bookingId}")
    BookingDto getBooking(@PathVariable("bookingId") Long bookingId);

    @PutMapping("/{bookingId}/mark-paid")
    BookingDto markAsPaid(@PathVariable("bookingId") Long bookingId);
}