package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ProfileUpdateDto;

public interface UserService {
    AuthResponseDto findUserById(Long userId);

    AuthResponseDto findUserByEmail(String email);

    AuthResponseDto updateProfile(Long userId, ProfileUpdateDto profileUpdateDto);
}
