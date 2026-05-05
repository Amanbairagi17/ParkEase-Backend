package com.parkease.payment_service.controller;

import com.parkease.payment_service.dtos.PaymentRequestDto;
import com.parkease.payment_service.dtos.PaymentResponseDto;
import com.parkease.payment_service.dtos.PaymentVerificationDto;
import com.parkease.payment_service.dtos.RazorpayOrderDto;
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
    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderDto> createOrder(@RequestBody PaymentRequestDto requestDto) {
        log.info("Create order request. bookingId={}, ", requestDto.getBookingId());
        return ResponseEntity.ok(paymentService.createRazorpayOrder(requestDto));
    }

    @PreAuthorize("hasAnyRole('DRIVER','ADMIN')")
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyPayment(@RequestBody PaymentVerificationDto dto) {
        log.info("Verify payment for orderId={}", dto.getRazorpayOrderId());
        return ResponseEntity.ok(paymentService.verifyPayment(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/manual")
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.processPayment(requestDto));
    }

    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isBookingOwner(#bookingId)")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponseDto> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getByBookingId(bookingId));
    }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#paymentId)")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponseDto> refundPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    @PreAuthorize("hasRole('ADMIN') or @paymentSecurity.isOwner(#paymentId)")
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<String> getPaymentStatus(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getTransactionHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getTransactionHistory(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PaymentResponseDto>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    @GetMapping("/revenue/{userId}")
    public ResponseEntity<BigDecimal> getTotalRevenue(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getTotalRevenueForUser(userId));
    }
}