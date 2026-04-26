package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.AuthResponseDto;

public interface UserService {
    AuthResponseDto findUserById(Long userId);

    AuthResponseDto findUserByEmail(String email);
}
