package com.parkease.booking_service.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingEstimateResponseDto {

    private Long lotId;
    private Long spotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalAmount;
    private BigDecimal hourlyRate;
    private long durationMinutes;
}

