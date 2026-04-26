package com.parkease.payment_service.controller;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @PostMapping
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        log.info("Payment request received. bookingId={}, userId={}", requestDto.getBookingId(), requestDto.getUserId());
        PaymentResponseDto response = paymentService.processPayment(requestDto);
        log.info("Payment processed. paymentId={}", response.getPaymentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponseDto> getByBookingId(@PathVariable Long bookingId) {
        log.info("Fetching payment for bookingId={}", bookingId);
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getByUserId(@PathVariable Long userId) {
        log.info("Fetching payments for userId={}", userId);
        return ResponseEntity.ok(paymentService.getByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#paymentId)")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponseDto> refundPayment(@PathVariable Long paymentId) {
        log.info("Refund request for paymentId={}", paymentId);
        PaymentResponseDto response = paymentService.refundPayment(paymentId);
        log.info("Refund processed. paymentId={}", paymentId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<String> getPaymentStatus(@PathVariable Long paymentId) {
        log.info("Fetching payment status. paymentId={}", paymentId);
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getTransactionHistory(@PathVariable Long userId) {
        log.info("Fetching transaction history for userId={}", userId);
        return ResponseEntity.ok(paymentService.getTransactionHistory(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        log.info("Admin fetching all payments");
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @GetMapping("/revenue/{userId}")
    public ResponseEntity<BigDecimal> getTotalRevenue(@PathVariable Long userId) {
        log.info("Fetching total revenue for userId={}", userId);
        return ResponseEntity.ok(paymentService.getTotalRevenueForUser(userId));
    }
}
