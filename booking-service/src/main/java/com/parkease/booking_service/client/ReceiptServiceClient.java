package com.parkease.booking_service.client;

import com.parkease.booking_service.dtos.ReceiptResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "RECEIPT-SERVICE", path = "/api/receipts")
public interface ReceiptServiceClient {

    @GetMapping("/payment/{paymentId}")
    ReceiptResponseDto getByPaymentId(@PathVariable("paymentId") Long paymentId);
}
