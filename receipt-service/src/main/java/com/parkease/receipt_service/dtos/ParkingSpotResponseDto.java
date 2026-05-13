package com.parkease.receipt_service.dtos;

import lombok.Data;

@Data
public class ParkingSpotResponseDto {
    private Long spotId;
    private Long lotId;
    private String spotNumber;
}
