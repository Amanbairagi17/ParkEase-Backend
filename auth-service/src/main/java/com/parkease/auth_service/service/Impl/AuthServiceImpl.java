package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.*;
import com.parkease.auth_service.entity.*;
import com.parkease.auth_service.exception.OtpException;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.mapper.Impl.SignUpMapper;
import com.parkease.auth_service.repository.*;
import com.parkease.auth_service.service.AuthService;
import com.parkease.auth_service.service.UserOtpService;
import com.parkease.auth_service.service.UserVerificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailsService userDetailsService;



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
        userVerification.setUserId(savedUser.getUserId());
        userVerification.setToken(token);

        UserVerification savedUserVerification =  userVerificationRepository.save(userVerification);

        String verificationLink = domainUrl + "/auth/verify/" + savedUserVerification.getToken();

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUserVerification.getToken());

        AuthResponseDto response = authResponseMapper.mapTo(savedUser);
        response.setMessage("User register successfully !!, OTP sent to email.");

        log.info("User registered successfully. userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());
        return response;
    }

    @Override
    @Transactional
    public LoginResponseDto login(LoginDto loginDto) {

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

        String accessToken = jwtService.generateToken(userDetails);

        log.info("JWT token generated successfully for email={}", loginDto.getEmail());
        log.info("Authorities assigned: {}",
                userDetails.getAuthorities()
                        .stream()
                        .map(a -> a.getAuthority())
                        .toList());

        refreshTokenRepository.deleteByUserId(userDetails.getUserId());

        // create new refresh token
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userDetails.getUserId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(refreshToken);

        log.info("JWT + Refresh token generated successfully for email={}", loginDto.getEmail());


        return new LoginResponseDto(accessToken, refreshTokenValue);

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

        UserOtp userOtp = userOtpRepository.findByUserId(user.getUserId())
                .orElseThrow(() ->  new OtpException("No otp for user " + user.getUserId()));

        if(userOtp.getLastOtpDateTime().plusMinutes(5).isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed due to expired OTP for email {}", resetPasswordDto.getEmail());
            throw new OtpException("OTP expired");
        }

        if(!Objects.equals(userOtp.getUserId(), user.getUserId())) {
            log.warn("Password reset failed due to OTP ownership mismatch for user {}", user.getUserId());
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
        log.info("Password reset completed for user {}", user.getUserId());
    }

    @Override
    public void sendOtp(String email) {
        log.info("Password reset OTP requested for email {}", email);
        userOtpService.sendOtp(email);
    }

    @Override
    @Transactional
    public LoginResponseDto refreshToken(String refreshTokenValue) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = authRepository.findById(token.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(user.getEmail());

        refreshTokenRepository.delete(token);

        String newRefreshToken = UUID.randomUUID().toString();

        RefreshToken newToken = new RefreshToken();
        newToken.setUserId(user.getUserId());
        newToken.setToken(newRefreshToken);
        newToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(newToken);

        String newAccessToken = jwtService.generateToken(userDetails);

        return new LoginResponseDto(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshRequestDto refreshToken) {

        log.info("Logout request received");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        refreshTokenRepository.delete(token);

        log.info("Refresh token deleted successfully, user logged out");
    }
}
