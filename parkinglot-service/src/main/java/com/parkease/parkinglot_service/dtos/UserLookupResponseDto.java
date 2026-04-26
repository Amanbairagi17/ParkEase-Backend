package com.parkease.parkinglot_service.dtos;

import lombok.Data;

@Data
public class UserLookupResponseDto {
    private String fullName;
    private String email;
    private String role;
    private String message;
}
