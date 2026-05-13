package com.parkease.receipt_service.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReceiptNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generate(Long paymentId, String receiptId) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        if (paymentId != null) {
            return "RCPT-" + datePart + "-" + String.format("%06d", paymentId);
        }
        String suffix = receiptId == null ? "000000" : receiptId.replace("-", "").substring(0, 6).toUpperCase();
        return "RCPT-" + datePart + "-" + suffix;
    }
}
