package com.parkease.booking_service.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingEstimateRequestDto {

    @NotNull(message = "Lot id is required")
    @Positive(message = "Lot id must be greater than 0")
    private Long lotId;

    @NotNull(message = "Spot id is required")
    @Positive(message = "Spot id must be greater than 0")
    private Long spotId;

    private com.parkease.booking_service.entity.BookingType bookingType;

    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}

