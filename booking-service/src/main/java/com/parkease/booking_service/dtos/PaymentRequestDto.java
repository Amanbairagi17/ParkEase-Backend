package com.parkease.booking_service.dtos;

import com.parkease.booking_service.entity.BookingType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDto {
    private Long bookingId;
    private String mode;
    private BigDecimal amount;
    private String currency = "INR";
    private String description;
    private BookingType bookingType;
    private String idempotencyKey;
}
