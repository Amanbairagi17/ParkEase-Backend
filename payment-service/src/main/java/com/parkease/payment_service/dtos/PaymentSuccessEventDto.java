package com.parkease.payment_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEventDto {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private java.math.BigDecimal amount;
    private String transactionId;
    private String paymentMode;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status;
}
