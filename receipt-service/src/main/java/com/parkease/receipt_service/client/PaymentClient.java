package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PAYMENT-SERVICE", path = "/api/internal/payments")
public interface PaymentClient {

    @GetMapping("/{paymentId}")
    PaymentResponseDto getPaymentById(
            @PathVariable("paymentId") Long paymentId
    );
}