package com.parkease.receipt_service.dtos;

import lombok.Data;

@Data
public class ParkingLotResponseDto {
    private Long lotId;
    private String name;
    private String address;
    private String city;
}
