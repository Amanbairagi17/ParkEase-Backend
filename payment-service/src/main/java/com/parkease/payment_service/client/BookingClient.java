package com.parkease.payment_service.client;

import com.parkease.payment_service.dtos.BookingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "booking-service", url = "${booking-service.url:http://localhost:8085}/api/bookings")
public interface BookingClient {

    @GetMapping("/{bookingId}")
    BookingDto getBooking(@PathVariable("bookingId") Long bookingId);
}
