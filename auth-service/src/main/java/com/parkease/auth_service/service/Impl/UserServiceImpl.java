package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ImageUploadResponse;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.MediaService;
import com.parkease.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final AuthResponseMapper authResponseMapper;
    private final MediaService mediaService;

    @Override
    public AuthResponseDto findUserById(Long userId) {
        log.info("Fetching user details for userId={}", userId);
        User user = repository.findById(userId).orElseThrow(() -> {
            log.error("Error: User not found for userId={}", userId);
            return new UsernameNotFoundException("user not exist with this Id : " +userId);
        });
        log.info("Successfully fetched user details for userId={}", userId);
        return authResponseMapper.mapTo(user);
    }

    @Override
    public AuthResponseDto findUserByEmail(String email) {
        log.info("Fetching user details for email={}", email);
        User user = repository.findByEmail(email).orElseThrow(() -> {
            log.error("Error: User not found for email={}", email);
            return new UsernameNotFoundException("user not exist with this email : " + email);
        });
        log.info("Successfully fetched user details for email={}", email);
        return authResponseMapper.mapTo(user);
    }

    @Override
    @Transactional
    public AuthResponseDto updateProfile(Long userId, ProfileUpdateDto dto) {

        log.info("Updating profile for userId={}", userId);

        User user = repository.findById(userId).orElseThrow(() -> {
            log.error("User not found for userId={}", userId);
            return new UsernameNotFoundException("User not exist with id: " + userId);
        });

        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        if (dto.getProfileImageFile() != null && !dto.getProfileImageFile().isEmpty()) {
            log.info("File received: {}", dto.getProfileImageFile());
            log.info("Is empty: {}", dto.getProfileImageFile() != null ? dto.getProfileImageFile().isEmpty() : "NULL");

            //  delete old image if exists
            if (user.getProfilePicPublicId() != null) {
                mediaService.deleteImage(user.getProfilePicPublicId());
            }

            // upload new image
            ImageUploadResponse uploadResponse =
                    mediaService.uploadImage(dto.getProfileImageFile());

            // save new image detail
            user.setProfilePicUrl(uploadResponse.getImageUrl());
            user.setProfilePicPublicId(uploadResponse.getPublicId());
        }

        User updatedUser = repository.save(user);

        log.info("Successfully updated profile for userId={}", userId);

        AuthResponseDto response = authResponseMapper.mapTo(updatedUser);
        response.setMessage("Profile updated successfully");

        return response;
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {

        User user = repository.findById(userId).orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + userId)
        );

        // delete from Cloudinary
        if (user.getProfilePicPublicId() != null) {
            mediaService.deleteImage(user.getProfilePicPublicId());
        }

        // remove from DB
        user.setProfilePicUrl(null);
        user.setProfilePicPublicId(null);

        repository.save(user);
    }


    @Override
    @Transactional
    public AuthResponseDto updateProfilePicture(Long userId, MultipartFile file) {

        log.info("Updating profile picture for userId={}", userId);

        User user = repository.findById(userId).orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + userId)
        );

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Profile image is required");
        }

        // delete old image
        if (user.getProfilePicPublicId() != null) {
            mediaService.deleteImage(user.getProfilePicPublicId());
        }

        // upload new image
        ImageUploadResponse response = mediaService.uploadImage(file);

        // update only image fields
        user.setProfilePicUrl(response.getImageUrl());
        user.setProfilePicPublicId(response.getPublicId());

        repository.save(user);

        log.info("Profile picture updated for userId={}", userId);

        AuthResponseDto responseDto = authResponseMapper.mapTo(user);
        responseDto.setMessage("Profile picture updated successfully");

        return responseDto;
    }
}
