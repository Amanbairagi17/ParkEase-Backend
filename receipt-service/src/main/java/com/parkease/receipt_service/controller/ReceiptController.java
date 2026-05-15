package com.parkease.receipt_service.controller;

import com.parkease.receipt_service.dtos.ReceiptResponseDto;
import com.parkease.receipt_service.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
@Slf4j
public class ReceiptController {

    private final ReceiptService receiptService;

    @PreAuthorize("hasRole('ADMIN') or @receiptSecurity.isPaymentOwner(#paymentId)")
    @PostMapping("/generate/{paymentId}")
    public ResponseEntity<ReceiptResponseDto> generateReceipt(@PathVariable Long paymentId) {
        log.info("Manual receipt generation for paymentId={}", paymentId);
        return ResponseEntity.ok(receiptService.generateReceipt(paymentId));
    }

    @PreAuthorize("hasRole('ADMIN') or @receiptSecurity.isPaymentOwner(#paymentId)")
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<ReceiptResponseDto> getReceiptByPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(receiptService.getReceiptByPayment(paymentId));
    }

    @PreAuthorize("hasRole('ADMIN') or @receiptSecurity.isOwner(#receiptId)")
    @GetMapping("/{receiptId}")
    public ResponseEntity<ReceiptResponseDto> getReceipt(@PathVariable String receiptId) {
        return ResponseEntity.ok(receiptService.getReceipt(receiptId));
    }

    @PreAuthorize("hasRole('ADMIN') or @receiptSecurity.isCurrentUser(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReceiptResponseDto>> getReceiptsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(receiptService.getReceiptsByUser(userId));
    }
}
