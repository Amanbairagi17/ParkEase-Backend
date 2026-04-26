package com.parkease.auth_service.dtos;

import lombok.Data;

@Data
public class AuthResponseDto {
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private String message;
}
