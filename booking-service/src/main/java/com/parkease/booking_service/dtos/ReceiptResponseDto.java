package com.parkease.booking_service.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReceiptResponseDto {
    private String receiptId;
    private String receiptNumber;
    private Long paymentId;
    private Long bookingId;
    private String downloadUrl;
    private LocalDateTime generatedAt;
}
