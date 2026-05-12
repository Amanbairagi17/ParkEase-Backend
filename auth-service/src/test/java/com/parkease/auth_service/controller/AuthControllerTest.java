package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.LoginDto;
import com.parkease.auth_service.dtos.LoginResponseDto;
import com.parkease.auth_service.dtos.RefreshRequestDto;
import com.parkease.auth_service.dtos.ResetPasswordDto;
import com.parkease.auth_service.dtos.SignUpDto;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.service.AuthService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Test
    void register_ShouldReturnCreated() {

        SignUpDto dto = buildSignUpDto();

        AuthResponseDto authResponseDto = new AuthResponseDto();
        authResponseDto.setMessage("User registered");

        when(authService.registerUser(any(SignUpDto.class)))
                .thenReturn(authResponseDto);

        ResponseEntity<AuthResponseDto> response =
                authController.register(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "User registered",
                response.getBody().getMessage()
        );

        verify(authService).registerUser(any(SignUpDto.class));
    }

    @Test
    void login_ShouldReturnCreated() {

        LoginDto dto = new LoginDto();
        dto.setEmail("user@demo.com");
        dto.setPassword("Password@123");

        LoginResponseDto loginResponseDto =
                new LoginResponseDto("access-token", "refresh-token");

        when(authService.login(any(LoginDto.class)))
                .thenReturn(loginResponseDto);

        ResponseEntity<LoginResponseDto> response =
                authController.login(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "access-token",
                response.getBody().getAccessToken()
        );

        verify(authService).login(any(LoginDto.class));
    }

    @Test
    void verify_ShouldReturnOk() {

        doNothing().when(authService).verify("token");

        ResponseEntity<String> response =
                authController.verifyUser("token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Email account verified successfully, you can login now",
                response.getBody()
        );

        verify(authService).verify("token");
    }

    @Test
    void sendOtp_ShouldReturnOk() {

        doNothing().when(authService)
                .sendOtp("user@demo.com");

        ResponseEntity<String> response =
                authController.sendOtp("user@demo.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "OTP sent to email",
                response.getBody()
        );

        verify(authService).sendOtp("user@demo.com");
    }

    @Test
    void resetPassword_ShouldReturnOk() {

        ResetPasswordDto dto =
                new ResetPasswordDto(
                        "user@demo.com",
                        "123456",
                        "NewPassword@123"
                );

        doNothing().when(authService)
                .resetPassword(any(ResetPasswordDto.class));

        ResponseEntity<String> response =
                authController.resetPassword(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Password reset successful",
                response.getBody()
        );

        verify(authService)
                .resetPassword(any(ResetPasswordDto.class));
    }

    @Test
    void refresh_ShouldReturnOk() {

        RefreshRequestDto dto = new RefreshRequestDto();
        dto.setRefreshToken("refresh-token");

        LoginResponseDto loginResponseDto =
                new LoginResponseDto(
                        "new-access-token",
                        "refresh-token"
                );

        when(authService.refreshToken("refresh-token"))
                .thenReturn(loginResponseDto);

        ResponseEntity<LoginResponseDto> response =
                authController.refresh(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "new-access-token",
                response.getBody().getAccessToken()
        );

        verify(authService).refreshToken("refresh-token");
    }

    @Test
    void logout_ShouldReturnOk() {

        RefreshRequestDto dto = new RefreshRequestDto();
        dto.setRefreshToken("refresh-token");

        doNothing().when(authService)
                .logout(any(RefreshRequestDto.class));

        ResponseEntity<String> response =
                authController.logout(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Logged out successfully",
                response.getBody()
        );

        verify(authService)
                .logout(any(RefreshRequestDto.class));
    }

    private SignUpDto buildSignUpDto() {

        SignUpDto dto = new SignUpDto();

        dto.setFullName("Test User");
        dto.setEmail("user@demo.com");
        dto.setPassword("Password@123");
        dto.setPhone("9876543210");
        dto.setRole(Role.DRIVER);

        return dto;
    }
}