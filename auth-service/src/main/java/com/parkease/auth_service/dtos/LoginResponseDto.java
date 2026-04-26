package com.parkease.auth_service.dtos;

import lombok.Data;

@Data
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;

    //this response dto for returning response
    public LoginResponseDto(String accessToken, String refreshToken){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
