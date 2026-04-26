package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.*;
import com.parkease.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping(path = "/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody SignUpDto signUpDto){
        log.info("Request received to register user. email={}", signUpDto.getEmail());
        AuthResponseDto response = authService.registerUser(signUpDto);
        log.info("Returning response for user registration. email={}", signUpDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public  ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        log.info("Request received for user login. email={}", loginDto.getEmail());
        LoginResponseDto response = authService.login(loginDto);
        log.info("Returning response for user login. email={}", loginDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<String> verifyUser(@PathVariable String token) {

        authService.verify(token);

        return ResponseEntity.ok("Email account verified successfully, you can login now");
    }

    // SEND OTP
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        authService.sendOtp(email);
        return ResponseEntity.ok("OTP sent to email");
    }

    // RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordDto dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok("Password reset successful");
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshRequestDto request) {

        LoginResponseDto newAccessToken = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(newAccessToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshRequestDto refreshToken) {

        authService.logout(refreshToken);

        return ResponseEntity.ok("Logged out successfully");
    }

}
