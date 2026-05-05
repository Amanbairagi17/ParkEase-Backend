package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    AuthResponseDto findUserById(Long userId);

    AuthResponseDto findUserByEmail(String email);

    AuthResponseDto updateProfile(Long userId, ProfileUpdateDto profileUpdateDto);

    void deleteProfileImage(Long userId);

    AuthResponseDto updateProfilePicture(Long userId, MultipartFile file);
}
