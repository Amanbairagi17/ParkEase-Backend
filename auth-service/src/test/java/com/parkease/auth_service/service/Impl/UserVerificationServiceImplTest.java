package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.UserVerification;
import com.parkease.auth_service.exception.TokenNotFoundException;
import com.parkease.auth_service.repository.UserVerificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserVerificationServiceImplTest {

    @Mock
    private UserVerificationRepository userVerificationRepository;

    @InjectMocks
    private UserVerificationServiceImpl service;

    @Test
    void findByToken_ShouldReturnEntity_WhenFound() {
        UserVerification verification = new UserVerification();
        verification.setToken("token");
        when(userVerificationRepository.findByToken("token")).thenReturn(Optional.of(verification));

        UserVerification result = service.findByToken("token");

        assertSame(verification, result);
    }

    @Test
    void findByToken_ShouldThrow_WhenMissing() {
        when(userVerificationRepository.findByToken("token")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> service.findByToken("token"));
    }
}
