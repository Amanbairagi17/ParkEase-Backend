package com.parkease.receipt_service.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String mode;
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String currency;
    private LocalDateTime paidAt;
}
