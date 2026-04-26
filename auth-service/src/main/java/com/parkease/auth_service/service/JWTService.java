package com.parkease.auth_service.service;

import com.parkease.auth_service.entity.CustomUserDetails;

public interface JWTService {
    String generateToken(CustomUserDetails userDetails);

    String extractUserName(String token);

    boolean validateToken(String token, CustomUserDetails userDetails);
}
