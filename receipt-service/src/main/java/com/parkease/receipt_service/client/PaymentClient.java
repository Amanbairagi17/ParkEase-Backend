package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service", url = "${payment-service.url:http://localhost:8087}/api/payments")
public interface PaymentClient {

    @GetMapping("/{paymentId}")
    PaymentResponseDto getPaymentById(@PathVariable("paymentId") Long paymentId);
}
