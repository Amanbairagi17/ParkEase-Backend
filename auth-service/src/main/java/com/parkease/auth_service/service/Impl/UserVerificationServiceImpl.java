package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.UserVerification;
import com.parkease.auth_service.exception.TokenNotFoundException;
import com.parkease.auth_service.repository.UserVerificationRepository;
import com.parkease.auth_service.service.UserVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVerificationServiceImpl implements UserVerificationService {
    private final UserVerificationRepository userVerificationRepository;
    @Override
    public UserVerification findByToken(String token) {
        return userVerificationRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Token " + token + " not found!"));
    }
}
