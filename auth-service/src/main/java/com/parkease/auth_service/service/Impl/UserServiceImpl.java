package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final AuthResponseMapper authResponseMapper;

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
    public AuthResponseDto updateProfile(Long userId, ProfileUpdateDto profileUpdateDto) {
        log.info("Updating profile for userId={}", userId);
        User user = repository.findById(userId).orElseThrow(() -> {
            log.error("Error: User not found for userId={}", userId);
            return new UsernameNotFoundException("user not exist with this Id : " + userId);
        });

        user.setFullName(profileUpdateDto.getFullName());
        user.setEmail(profileUpdateDto.getEmail());
        user.setPhone(profileUpdateDto.getPhone());
        user.setAddress(profileUpdateDto.getAddress());
        if (profileUpdateDto.getProfilePicUrl() != null) {
            user.setProfilePicUrl(profileUpdateDto.getProfilePicUrl());
        }

        User updatedUser = repository.save(user);
        log.info("Successfully updated profile for userId={}", userId);
        
        AuthResponseDto response = authResponseMapper.mapTo(updatedUser);
        response.setMessage("Profile updated successfully");
        return response;
    }
}
