package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.UserVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserVerificationRepository extends JpaRepository<UserVerification, Long> {
    Optional<UserVerification> findByToken(String token);

    Optional<UserVerification> findByUserId(Integer userId);

    void deleteByUserId(Integer userId);
}
