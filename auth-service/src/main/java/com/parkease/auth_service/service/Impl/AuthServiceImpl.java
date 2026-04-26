package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.LoginDto;
import com.parkease.auth_service.dtos.ResetPasswordDto;
import com.parkease.auth_service.dtos.SignUpDto;
import com.parkease.auth_service.entity.CustomUserDetails;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.entity.UserOtp;
import com.parkease.auth_service.entity.UserVerification;
import com.parkease.auth_service.exception.OtpException;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.mapper.Impl.SignUpMapper;
import com.parkease.auth_service.repository.AuthRepository;
import com.parkease.auth_service.repository.UserOtpRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.repository.UserVerificationRepository;
import com.parkease.auth_service.service.AuthService;
import com.parkease.auth_service.service.UserOtpService;
import com.parkease.auth_service.service.UserVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${domain.url}")
    private String domainUrl;

    private final AuthRepository authRepository;
    private final SignUpMapper signUpMapper;
    private final AuthResponseMapper authResponseMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JWTServiceImpl jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserOtpService userOtpService;
    private final UserVerificationRepository userVerificationRepository;
    private final EmailServiceImpl emailService;
    private final UserVerificationService userVerificationService;
    private final UserRepository userRepository;
    private final UserOtpRepository userOtpRepository;



    public AuthResponseDto registerUser(SignUpDto signUpDto) {
        log.info("Starting user registration for email={}", signUpDto.getEmail());
        if (authRepository.existsByEmail(signUpDto.getEmail())) {
            log.error("Registration failed. User already exists with email={}", signUpDto.getEmail());
            throw new RuntimeException("User already exist by given email Id, Go for login: " + signUpDto.getEmail());
        }

        User user = signUpMapper.mapFrom(signUpDto);
        user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));
        user.setIsActive(false);

        User savedUser = authRepository.save(user);

        String token = UUID.randomUUID().toString().substring(0, 6);

        UserVerification userVerification = new UserVerification();
        userVerification.setUserId(savedUser.getId());
        userVerification.setToken(token);

        UserVerification savedUserVerification =  userVerificationRepository.save(userVerification);

        String verificationLink = domainUrl + "/auth/verify/" + savedUserVerification.getToken();

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUserVerification.getToken());

        AuthResponseDto response = authResponseMapper.mapTo(savedUser);
        response.setMessage("User register successfully !!, OTP sent to email.");

        log.info("User registered successfully. userId={}, email={}", savedUser.getId(), savedUser.getEmail());
        return response;
    }

    @Override
    public String login(LoginDto loginDto) {

        log.info("Login request received for email={}", loginDto.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );

        log.info("Authentication successful for email={}", loginDto.getEmail());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        log.info("Generating JWT token for userId={}, roles={}",
                userDetails.getUserId(),
                userDetails.getAuthorities());

        String token = jwtService.generateToken(userDetails);

        log.info("JWT token generated successfully for email={}", loginDto.getEmail());
        log.info("Authorities assigned: {}",
                userDetails.getAuthorities()
                        .stream()
                        .map(a -> a.getAuthority())
                        .toList());

        return token;
    }

    @Override
    public void verify(String token) {
        UserVerification verification = userVerificationService.findByToken(token);

        User user = authRepository.findById(verification.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(true);
        authRepository.save(user);

        userVerificationRepository.delete(verification);
    }

    @Override
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        log.info("Password reset requested for email {}", resetPasswordDto.getEmail());
        User user = userRepository.findByEmail(resetPasswordDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email " + resetPasswordDto.getEmail()));

        UserOtp userOtp = userOtpRepository.findByUserId(user.getId())
                .orElseThrow(() ->  new OtpException("No otp for user " + user.getId()));

        if(userOtp.getLastOtpDateTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed due to expired OTP for email {}", resetPasswordDto.getEmail());
            throw new OtpException("OTP expired");
        }

        if(!Objects.equals(userOtp.getUserId(), user.getId())) {
            log.warn("Password reset failed due to OTP ownership mismatch for user {}", user.getId());
            throw new OtpException("Internal error try resending otp");
        }

        if(!userOtp.getOtp().equals(resetPasswordDto.getOtp())) {
            log.warn("Password reset failed due to invalid OTP for email {}", resetPasswordDto.getEmail());
            throw new OtpException("Invalid otp");
        }

        /*
         reset the otp to a random very large otp so the user cannot user the same
         otp to change the password again
        */
        userOtp.setOtp(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset completed for user {}", user.getId());
    }

    @Override
    public void sendOtp(String email) {
        log.info("Password reset OTP requested for email {}", email);
        userOtpService.sendOtp(email);
    }
}
