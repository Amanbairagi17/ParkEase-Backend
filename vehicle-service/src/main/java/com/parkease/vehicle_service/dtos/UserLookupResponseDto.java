package com.parkease.vehicle_service.dtos;

import lombok.Data;

@Data
public class UserLookupResponseDto {
    private Long ownerId;
    private String fullName;
    private String email;
    private String message;
}
