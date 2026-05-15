package com.parkease.booking_service.dtos;

import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.entity.PricingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequestDto {

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be greater than 0")
    private Long userId;

    @NotNull(message = "Lot id is required")
    @Positive(message = "Lot id must be greater than 0")
    private Long lotId;

    @NotNull(message = "Spot id is required")
    @Positive(message = "Spot id must be greater than 0")
    private Long spotId;

    @NotBlank(message = "Vehicle plate is required")
    private String vehiclePlate;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @NotNull(message = "Booking type is required")
    private BookingType bookingType;

    @NotNull(message = "Pricing type is required")
    private PricingType pricingType;

    // For WALK_IN_BOOKING: startTime is optional — backend auto-sets it to current time
    // For PRE_BOOKING: startTime is required and must be in the future
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String email;
}