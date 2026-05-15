package com.parkease.payment_service.dtos;

import com.parkease.payment_service.entity.PaymentMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "payment mode is required")
    private PaymentMode mode;

    private String description;
    private String currency = "INR";
    private BigDecimal amount;
    private String idempotencyKey;
}
