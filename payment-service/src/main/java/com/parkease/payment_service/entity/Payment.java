package com.parkease.payment_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode mode;

    private String transactionId;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    @Column(nullable = false)
    private String currency = "INR";

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private String description;
}
