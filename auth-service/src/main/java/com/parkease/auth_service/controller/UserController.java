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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping(path = {"/user", "/api/user", "/users", "/api/users"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DRIVER','MANAGER','ADMIN')")
public class UserController {
    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponseDto> getUserById(@PathVariable Long userId){
        validateUserId(userId);
        log.info("Request received to fetch user by id. userId={}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<AuthResponseDto> getUserByEmail(@PathVariable String email) {
        log.info("Request received to fetch user by email. email={}", email);
        AuthResponseDto response = userService.findUserByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping(value = "/{userId}/profile", consumes = "multipart/form-data")
    public ResponseEntity<AuthResponseDto> updateProfile(
            @PathVariable Long userId,
            @Valid @ModelAttribute ProfileUpdateDto dto
    ) {
        validateUserId(userId);
        log.info("Updating profile for userId={}", userId);
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    @DeleteMapping("/{userId}/profile-image")
    public ResponseEntity<String> deleteProfileImage(@PathVariable Long userId) {
        validateUserId(userId);

        userService.deleteProfileImage(userId);

        return ResponseEntity.ok("Profile image deleted successfully");
    }

    @PatchMapping(value = "/{userId}/profile-picture", consumes = "multipart/form-data")
    public ResponseEntity<AuthResponseDto> updateProfilePicture(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        validateUserId(userId);
        return ResponseEntity.ok(userService.updateProfilePicture(userId, file));
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId");
        }
    }
}
