package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {
}
