package com.parkease.parkinglot_service.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ParkingLotRequestDto {

    @NotBlank(message = "Lot name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
    private Double longitude;

    @NotNull(message = "Total spots is required")
    @Positive(message = "Total spots must be greater than 0")
    private Integer totalSpots;

    @NotNull(message = "Manager id is required")
    @Positive(message = "Manager id must be greater than 0")
    private Long managerId;

    @NotNull(message = "Open status is required")
    private Boolean open;

    @NotNull(message = "Approval status is required")
    private Boolean approved;

    private LocalTime openTime;

    private LocalTime closeTime;

    private String imageUrl;
}
