package com.parkease.parkingspot_service.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.parkease.parkingspot_service.entity.SpotStatus;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ParkingSpotRequestDto {

    @NotNull(message = "Lot id is required")
    @Positive(message = "Lot id must be greater than zero")
    private Long lotId;

    @NotBlank(message = "Spot number is required")
    private String spotNumber;

    @NotNull(message = "Floor is required")
    private Integer floor;

    @NotNull(message = "Spot type is required")
    private SpotType spotType;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private SpotStatus status;

    @NotNull(message = "Handicapped flag is required")
    private Boolean handicapped;

    @NotNull(message = "EV charging flag is required")
    @JsonProperty("EVCharging")
    @JsonAlias({"evCharging", "isEVCharging", "ev_charging", "evcharging"})
    private Boolean EVCharging;

    @NotNull(message = "Price per hour is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than zero")
    private Double pricePerHour;
}
