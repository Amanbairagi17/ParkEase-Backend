package com.parkease.auth_service.service;

import com.parkease.auth_service.entity.UserVerification;

import java.util.Optional;

public interface UserVerificationService {
    UserVerification findByToken(String token);
}
