package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import com.parkease.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = {"/user", "/api/user", "/users", "/api/users"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DRIVER','MANAGER','ADMIN')")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponseDto> getUserById(@PathVariable Long userId){
        log.info("Request received to fetch user by id. userId={}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<AuthResponseDto> getUserByEmail(@PathVariable String email) {
        log.info("Request received to fetch user by email. email={}", email);
        AuthResponseDto response = userService.findUserByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<AuthResponseDto> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileUpdateDto profileUpdateDto) {
        log.info("Request received to update profile for userId={}", userId);
        AuthResponseDto response = userService.updateProfile(userId, profileUpdateDto);
        return ResponseEntity.ok(response);
    }
}
