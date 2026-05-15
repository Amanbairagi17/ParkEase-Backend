package com.parkease.payment_service.controller;

import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                paymentService.getByPaymentId(paymentId)
        );
    }
}