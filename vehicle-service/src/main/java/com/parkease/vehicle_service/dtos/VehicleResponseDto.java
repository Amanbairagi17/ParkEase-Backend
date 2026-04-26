package com.parkease.vehicle_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
public class VehicleResponseDto {

    @NotNull
    private Long vehicleId;

    @NotNull
    private Long ownerId;

    @NotBlank
    private String licensePlate;

    @NotBlank
    private String make;

    @NotBlank
    private String model;

    @NotBlank
    private String color;

    @NotBlank
    private String vehicleType;

    @NotNull
    private Boolean isEV;

    private Boolean isActive;

    private LocalDateTime registeredAt;
}