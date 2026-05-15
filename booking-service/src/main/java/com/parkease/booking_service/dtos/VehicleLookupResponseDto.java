package com.parkease.booking_service.dtos;

import lombok.Data;

@Data
public class VehicleLookupResponseDto {
    private Long vehicleId;
    private Long ownerId;
    private String licensePlate;
    private String vehicleType;
    private Boolean active;
    private Boolean isActive;
}
