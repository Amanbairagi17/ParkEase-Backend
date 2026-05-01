package com.parkease.auth_service.dtos;

import lombok.Data;

@Data
public class WarnUserRequestDto {
    private String title;
    private String message;
}
