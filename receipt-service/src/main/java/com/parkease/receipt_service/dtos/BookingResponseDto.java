package com.parkease.receipt_service.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponseDto {
    private Long bookingId;
    private Long userId;
    private Long lotId;
    private Long spotId;
    private String vehiclePlate;
    private String vehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;
    private BigDecimal totalAmount;
}
