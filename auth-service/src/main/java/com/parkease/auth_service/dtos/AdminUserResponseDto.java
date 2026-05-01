package com.parkease.auth_service.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponseDto {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Boolean isActive;
    private String phone;
    private LocalDateTime createdAt;
    private String provider;
}
