package com.parkease.vehicle_service.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VehicleRequestDto {

    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    @NotBlank(message = "License plate is required")
    @Pattern(
            regexp = "^[A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}$",
            message = "Enter valid plate number (example: MP09AB1234)"
    )
    private String licensePlate;


    @NotBlank(message = "Vehicle make is required")
    @Size(min = 2, max = 30,
            message = "Make must be between 2 and 30 characters")
    private String make;


    @NotBlank(message = "Vehicle model is required")
    @Size(min = 1, max = 30,
            message = "Model must be between 1 and 30 characters")
    private String model;


    @NotBlank(message = "Color is required")
    @Pattern(
            regexp = "^[a-zA-Z ]+$",
            message = "Color should contain only alphabets"
    )
    private String color;


    @NotBlank(message = "Vehicle type is required")
    @Pattern(
            regexp = "^(2W|4W|HEAVY)$",
            message = "Vehicle type must be 2W, 4W or HEAVY"
    )
    private String vehicleType;

    @NotNull(message = "EV flag is required")
    private Boolean isEV;

}
