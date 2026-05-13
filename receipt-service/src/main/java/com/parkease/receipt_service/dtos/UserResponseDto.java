package com.parkease.receipt_service.dtos;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
}
