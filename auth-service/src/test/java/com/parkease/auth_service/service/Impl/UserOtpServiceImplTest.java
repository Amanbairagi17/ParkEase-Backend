package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.entity.UserOtp;
import com.parkease.auth_service.exception.OtpException;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.helper.AppConstants;
import com.parkease.auth_service.repository.UserOtpRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserOtpServiceImplTest {

    @Mock
    private UserOtpRepository userOtpRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserOtpServiceImpl userOtpService;

    @Test
    void sendOtp_ShouldThrow_WhenUserMissing() {
        when(userRepository.findByEmail("missing@demo.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userOtpService.sendOtp("missing@demo.com"));
    }

    @Test
    void sendOtp_ShouldCreateOtp_WhenFirstTime() {
        User user = buildUser();
        when(userRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(user.getUserId())).thenReturn(Optional.empty());

        userOtpService.sendOtp("user@demo.com");

        verify(userOtpRepository).save(any(UserOtp.class));
        verify(emailService).sendOtp(eq("user@demo.com"), anyString());
    }

    @Test
    void sendOtp_ShouldThrow_WhenResendBeforeCooldown() {
        User user = buildUser();
        UserOtp existing = buildOtp(user.getUserId());
        existing.setLastOtpDateTime(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(user.getUserId())).thenReturn(Optional.of(existing));

        assertThrows(OtpException.class, () -> userOtpService.sendOtp("user@demo.com"));
        verify(emailService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void sendOtp_ShouldThrow_WhenOtpLimitReached() {
        User user = buildUser();
        UserOtp existing = buildOtp(user.getUserId());
        existing.setLastOtpDateTime(LocalDateTime.now().minusMinutes(10));
        existing.setOtpSent(AppConstants.otpLimit);

        when(userRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(user.getUserId())).thenReturn(Optional.of(existing));

        assertThrows(OtpException.class, () -> userOtpService.sendOtp("user@demo.com"));
        verify(emailService, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void sendOtp_ShouldUpdateOtp_WhenAllowed() {
        User user = buildUser();
        UserOtp existing = buildOtp(user.getUserId());
        existing.setLastOtpDateTime(LocalDateTime.now().minusMinutes(10));
        existing.setOtpSent(1);

        when(userRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(user.getUserId())).thenReturn(Optional.of(existing));

        userOtpService.sendOtp("user@demo.com");

        verify(userOtpRepository).save(existing);
        verify(emailService).sendOtp(eq("user@demo.com"), anyString());
        assertEquals(2, existing.getOtpSent());
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@demo.com");
        return user;
    }

    private UserOtp buildOtp(Long userId) {
        UserOtp otp = new UserOtp();
        otp.setUserId(userId);
        otp.setOtp("123456");
        otp.setOtpSent(1);
        otp.setLastOtpDateTime(LocalDateTime.now().minusMinutes(10));
        return otp;
    }
}
