package com.parkease.booking_service.client;

import com.parkease.booking_service.dtos.PaymentRequestDto;
import com.parkease.booking_service.dtos.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PAYMENT-SERVICE", path = "/api/payments")
public interface PaymentServiceClient {

    @GetMapping("/booking/{bookingId}")
    PaymentResponseDto getByBookingId(@PathVariable("bookingId") Long bookingId);

    @PostMapping("/initialize")
    PaymentResponseDto initializePayment(@RequestBody PaymentRequestDto requestDto);

    @PostMapping("/booking/{bookingId}/refund")
    PaymentResponseDto refundByBookingId(@PathVariable("bookingId") Long bookingId);
}
