package com.parkease.parkinglot_service.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ParkingLotResponseDto {

    private Integer lotId;
    private String name;
    private String address;
    private String city;
    private Double latitude;
    private Double longitude;
    private Integer totalSpots;
    private Integer availableSpots;
    private Integer managerId;
    private Boolean open;
    private Boolean approved;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
