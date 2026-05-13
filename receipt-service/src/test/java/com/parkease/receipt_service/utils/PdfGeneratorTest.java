package com.parkease.receipt_service.utils;

import com.parkease.receipt_service.dtos.UserResponseDto;
import com.parkease.receipt_service.entity.Receipt;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGeneratorTest {

    private final PdfGenerator pdfGenerator = new PdfGenerator();

    @Test
    void generateReceiptPdfProducesBytes() {
        Receipt receipt = new Receipt();
        receipt.setReceiptId("test-id");
        receipt.setReceiptNumber("RCPT-20260512-000001");
        receipt.setBookingId(10L);
        receipt.setPaymentId(20L);
        receipt.setUserId(30L);
        receipt.setVehicleNumber("MH12AB1234");
        receipt.setParkingName("City Center Parking");
        receipt.setSlotNumber("A1");
        receipt.setCheckInTime(LocalDateTime.now().minusHours(2));
        receipt.setCheckOutTime(LocalDateTime.now());
        receipt.setDuration("2 hours");
        receipt.setAmountPaid(new BigDecimal("120.00"));
        receipt.setPaymentMethod("UPI");
        receipt.setPaymentStatus("SUCCESS");
        receipt.setTransactionId("txn_123");
        receipt.setRazorpayOrderId("order_123");
        receipt.setRazorpayPaymentId("pay_123");
        receipt.setGeneratedAt(LocalDateTime.now());

        UserResponseDto user = new UserResponseDto();
        user.setFullName("Test User");
        user.setEmail("test@example.com");

        byte[] pdfBytes = pdfGenerator.generateReceiptPdf(receipt, user);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
