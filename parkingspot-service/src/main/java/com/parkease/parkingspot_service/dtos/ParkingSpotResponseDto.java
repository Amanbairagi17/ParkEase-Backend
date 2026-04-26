package com.parkease.parkingspot_service.dtos;

import com.parkease.parkingspot_service.entity.SpotStatus;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ParkingSpotResponseDto {

    private Long spotId;
    private Long lotId;
    private String spotNumber;
    private Integer floor;
    private SpotType spotType;
    private VehicleType vehicleType;
    private SpotStatus status;
    private Boolean handicapped;
    private Boolean EVCharging;
    private Double pricePerHour;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
