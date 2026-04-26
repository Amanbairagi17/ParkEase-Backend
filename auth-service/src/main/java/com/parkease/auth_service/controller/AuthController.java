package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.LoginDto;
import com.parkease.auth_service.dtos.SignUpDto;
import com.parkease.auth_service.service.Impl.AuthServiceImpl;
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

    private final AuthServiceImpl authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody SignUpDto signUpDto){
        log.info("Request received to register user. email={}", signUpDto.getEmail());
        AuthResponseDto response = authService.registerUser(signUpDto);
        log.info("Returning response for user registration. email={}", signUpDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public  ResponseEntity<String> login(@Valid @RequestBody LoginDto loginDto) {
        log.info("Request received for user login. email={}", loginDto.getEmail());
        String response = authService.login(loginDto);
        log.info("Returning response for user login. email={}", loginDto.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<String> verifyUser(@PathVariable String token) {

        authService.verify(token);

        return ResponseEntity.ok("Email verified successfully");
    }

}
