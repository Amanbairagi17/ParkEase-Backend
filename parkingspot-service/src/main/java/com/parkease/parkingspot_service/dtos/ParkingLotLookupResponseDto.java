package com.parkease.parkingspot_service.dtos;

import lombok.Data;

@Data
public class ParkingLotLookupResponseDto {
    private Long lotId;
    private String name;
    private String city;
    private Long managerId;
    private Boolean open;
    private String message;
}
