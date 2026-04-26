package com.parkease.auth_service.dtos;

import lombok.Data;

@Data
public class RefreshRequestDto {
    private String refreshToken;
}