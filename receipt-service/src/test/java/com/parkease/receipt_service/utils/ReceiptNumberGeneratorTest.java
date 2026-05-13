package com.parkease.receipt_service.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptNumberGeneratorTest {

    @Test
    void generateUsesPaymentIdWhenAvailable() {
        String receiptNumber = ReceiptNumberGenerator.generate(42L, "abc");
        assertTrue(receiptNumber.startsWith("RCPT-"));
        assertTrue(receiptNumber.endsWith("000042"));
    }

    @Test
    void generateFallsBackToReceiptId() {
        String receiptNumber = ReceiptNumberGenerator.generate(null, "123e4567-e89b-12d3-a456-426614174000");
        assertTrue(receiptNumber.startsWith("RCPT-"));
    }
}
