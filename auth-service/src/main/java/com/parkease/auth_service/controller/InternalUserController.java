package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponseDto> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.findUserById(userId));
    }
}