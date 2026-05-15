package com.parkease.booking_service.dtos;

import lombok.Data;

import java.time.LocalTime;

@Data
public class ParkingLotLookupResponseDto {
    private Long lotId;
    private String name;
    private Boolean open;
    private Boolean approved;
    private Integer availableSpots;
    private LocalTime openTime;
    private LocalTime closeTime;
}
