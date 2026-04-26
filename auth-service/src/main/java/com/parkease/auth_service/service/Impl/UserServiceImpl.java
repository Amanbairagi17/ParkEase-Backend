package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository repository;
    private final AuthResponseMapper authResponseMapper;

    public AuthResponseDto findUserById(Long userId) {
        log.info("Fetching user details for userId={}", userId);
        User user = repository.findById(userId).orElseThrow(() -> {
            log.error("Error: User not found for userId={}", userId);
            return new UsernameNotFoundException("user not exist with this Id : " +userId);
        });
        log.info("Successfully fetched user details for userId={}", userId);
        return authResponseMapper.mapTo(user);
    }

    public AuthResponseDto findUserByEmail(String email) {
        log.info("Fetching user details for email={}", email);
        User user = repository.findByEmail(email).orElseThrow(() -> {
            log.error("Error: User not found for email={}", email);
            return new UsernameNotFoundException("user not exist with this email : " + email);
        });
        log.info("Successfully fetched user details for email={}", email);
        return authResponseMapper.mapTo(user);
    }
}
