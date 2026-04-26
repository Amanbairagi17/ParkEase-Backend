package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.*;


public interface AuthService {
    AuthResponseDto registerUser(SignUpDto signUpDto);

    LoginResponseDto login(LoginDto loginDto);

    void verify(String token);

    void resetPassword(ResetPasswordDto dto);

    void sendOtp(String email);

    LoginResponseDto refreshToken(String refreshToken);

    void logout(RefreshRequestDto refreshToken);
}
