package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.LoginDto;
import com.parkease.auth_service.dtos.ResetPasswordDto;
import com.parkease.auth_service.dtos.SignUpDto;


public interface AuthService {
    AuthResponseDto registerUser(SignUpDto signUpDto);

    String login(LoginDto loginDto);

    void verify(String token);

    void resetPassword(ResetPasswordDto dto);

    void sendOtp(String email);
}
