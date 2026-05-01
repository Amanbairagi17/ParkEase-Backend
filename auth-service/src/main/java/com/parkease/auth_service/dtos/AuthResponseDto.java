package com.parkease.auth_service.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuthResponseDto {
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String address;
    private String profilePicUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String message;
}
