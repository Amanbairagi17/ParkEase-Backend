package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.entity.UserOtp;
import com.parkease.auth_service.exception.OtpException;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.helper.AppConstants;
import com.parkease.auth_service.repository.UserOtpRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.EmailService;
import com.parkease.auth_service.service.UserOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOtpServiceImpl implements UserOtpService {
    private final UserOtpRepository userOtpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public void sendOtp(String email) {
        log.info("OTP send requested for email {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User does not exist with email " + email));

        Optional<UserOtp> userOtpOptional = userOtpRepository.findByUserId(user.getId());

        String otp = UUID.randomUUID().toString().substring(0, 6);
        if(userOtpOptional.isEmpty()) {
            UserOtp userOtp = UserOtp.builder()
                    .userId(user.getId())
                    .otp(otp)
                    .otpSent(1)
                    .build();
            userOtpRepository.save(userOtp);
            emailService.sendOtp(email, otp);
            log.info("OTP sent to user {}", user.getId());
        }
        else {
            UserOtp userOtp = userOtpOptional.get();
            LocalDateTime now = LocalDateTime.now();

            if(userOtp.getLastOtpDateTime().plusMinutes(5).isAfter(now)) {
                log.warn("OTP resend blocked for user {}", user.getId());
                throw new OtpException("You can send new OTP after 5 minutes");
            }
            if(userOtp.getOtpSent() >= AppConstants.otpLimit) {
                log.warn("OTP limit reached for user {}", user.getId());
                throw new OtpException("Maximum OTP limit reached please try again when limit reset - tomorrow");
            }

            userOtp.setOtpSent(userOtp.getOtpSent()+1);
            userOtp.setOtp(otp);
            userOtpRepository.save(userOtp);
            emailService.sendOtp(user.getEmail(), otp);
            log.info("OTP resent to user {}", user.getId());
        }
    }
}
