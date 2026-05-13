package com.parkease.receipt_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEventDto {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;
    private String paymentMode;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status;
}
