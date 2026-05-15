package com.parkease.booking_service.dtos;

import lombok.Data;

@Data
public class ParkingSpotLookupResponseDto {

    private Long spotId;
    private Long lotId;
    private String status;
    private String spotNumber;
    private String vehicleType;
    private Double pricePerHour;
}
