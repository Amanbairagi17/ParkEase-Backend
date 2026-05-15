package com.parkease.receipt_service.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReceiptResponseDto {
    private String receiptId;
    private String receiptNumber;
    private Long bookingId;
    private Long paymentId;
    private Long userId;
    private String vehicleNumber;
    private String parkingName;
    private String slotNumber;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String duration;
    private String bookingType;
    private String pricingType;
    private BigDecimal baseAmount;
    private BigDecimal serviceCharge;
    private BigDecimal gstAmount;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime paymentTime;
    private LocalDateTime generatedAt;
}
