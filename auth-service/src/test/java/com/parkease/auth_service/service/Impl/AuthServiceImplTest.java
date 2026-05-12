package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.*;
import com.parkease.auth_service.entity.CustomUserDetails;
import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.entity.UserOtp;
import com.parkease.auth_service.entity.UserVerification;
import com.parkease.auth_service.exception.OtpException;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.mapper.Impl.SignUpMapper;
import com.parkease.auth_service.repository.AuthRepository;
import com.parkease.auth_service.repository.RefreshTokenRepository;
import com.parkease.auth_service.repository.UserOtpRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.repository.UserVerificationRepository;
import com.parkease.auth_service.service.UserOtpService;
import com.parkease.auth_service.service.UserVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;
    @Mock
    private SignUpMapper signUpMapper;
    @Mock
    private AuthResponseMapper authResponseMapper;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private JWTServiceImpl jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserOtpService userOtpService;
    @Mock
    private UserVerificationRepository userVerificationRepository;
    @Mock
    private EmailServiceImpl emailService;
    @Mock
    private UserVerificationService userVerificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserOtpRepository userOtpRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Test
    void registerUser_ShouldThrow_WhenDuplicateEmail() {
        SignUpDto dto = buildSignUp();
        when(authRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.registerUser(dto));

        assertTrue(ex.getMessage().contains("User already exist"));
        verify(authRepository, never()).save(any(User.class));
        verify(userVerificationRepository, never()).save(any(UserVerification.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void registerUser_ShouldSaveUserAndVerification_WhenEmailNotExists() {
        SignUpDto dto = buildSignUp();
        User mapped = buildUser();
        User savedUser = buildUser();
        savedUser.setUserId(10L);

        when(authRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(signUpMapper.mapFrom(dto)).thenReturn(mapped);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encoded");
        when(authRepository.save(any(User.class))).thenReturn(savedUser);
        when(userVerificationRepository.save(any(UserVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(authResponseMapper.mapTo(savedUser)).thenReturn(new AuthResponseDto());

        authService.registerUser(dto);

        verify(authRepository).save(mapped);
        ArgumentCaptor<UserVerification> verificationCaptor = ArgumentCaptor.forClass(UserVerification.class);
        verify(userVerificationRepository).save(verificationCaptor.capture());
        verify(emailService).sendVerificationEmail(eq(savedUser.getEmail()), eq(verificationCaptor.getValue().getToken()));
    }

    @Test
    void login_ShouldReturnTokens_WhenAuthenticated() {
        LoginDto dto = new LoginDto();
        dto.setEmail("user@demo.com");
        dto.setPassword("Pass@123");

        User user = buildUser();
        user.setUserId(77L);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access");

        LoginResponseDto response = authService.login(dto);

        assertEquals("access", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository).deleteByUserId(77L);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertEquals(77L, refreshTokenCaptor.getValue().getUserId());
    }

    @Test
    void verify_ShouldActivateUserAndDeleteToken_WhenValidToken() {
        UserVerification verification = new UserVerification();
        verification.setUserId(5L);
        verification.setToken("TOKEN");
        User user = buildUser();
        user.setUserId(5L);
        user.setIsActive(false);

        when(userVerificationService.findByToken("TOKEN")).thenReturn(verification);
        when(authRepository.findById(5L)).thenReturn(Optional.of(user));

        authService.verify("TOKEN");

        assertTrue(user.getIsActive());
        verify(authRepository).save(user);
        verify(userVerificationRepository).delete(verification);
    }

    @Test
    void verify_ShouldThrow_WhenUserMissing() {
        UserVerification verification = new UserVerification();
        verification.setUserId(5L);
        verification.setToken("TOKEN");

        when(userVerificationService.findByToken("TOKEN")).thenReturn(verification);
        when(authRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.verify("TOKEN"));
    }

    @Test
    void resetPassword_ShouldThrow_WhenUserMissing() {
        ResetPasswordDto dto = new ResetPasswordDto("user@demo.com", "123456", "NewPass@123");
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.resetPassword(dto));
    }

    @Test
    void resetPassword_ShouldThrow_WhenOtpExpired() {
        ResetPasswordDto dto = new ResetPasswordDto("user@demo.com", "123456", "NewPass@123");
        User user = buildUser();
        user.setUserId(9L);
        UserOtp otp = new UserOtp();
        otp.setUserId(9L);
        otp.setOtp("123456");
        otp.setLastOtpDateTime(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(9L)).thenReturn(Optional.of(otp));

        assertThrows(OtpException.class, () -> authService.resetPassword(dto));
    }

    @Test
    void resetPassword_ShouldThrow_WhenOtpMismatch() {
        ResetPasswordDto dto = new ResetPasswordDto("user@demo.com", "999999", "NewPass@123");
        User user = buildUser();
        user.setUserId(11L);
        UserOtp otp = new UserOtp();
        otp.setUserId(11L);
        otp.setOtp("123456");
        otp.setLastOtpDateTime(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(11L)).thenReturn(Optional.of(otp));

        assertThrows(OtpException.class, () -> authService.resetPassword(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrow_WhenOtpUserIdMismatch() {
        ResetPasswordDto dto = new ResetPasswordDto("user@demo.com", "123456", "NewPass@123");
        User user = buildUser();
        user.setUserId(11L);
        UserOtp otp = new UserOtp();
        otp.setUserId(99L);
        otp.setOtp("123456");
        otp.setLastOtpDateTime(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(11L)).thenReturn(Optional.of(otp));

        assertThrows(OtpException.class, () -> authService.resetPassword(dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldUpdatePassword_WhenOtpValid() {
        ResetPasswordDto dto = new ResetPasswordDto("user@demo.com", "123456", "NewPass@123");
        User user = buildUser();
        user.setUserId(12L);
        UserOtp otp = new UserOtp();
        otp.setUserId(12L);
        otp.setOtp("123456");
        otp.setLastOtpDateTime(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));
        when(userOtpRepository.findByUserId(12L)).thenReturn(Optional.of(otp));
        when(passwordEncoder.encode(dto.getNewPassword())).thenReturn("encoded");

        authService.resetPassword(dto);

        verify(userRepository).save(user);
        assertEquals("encoded", user.getPassword());
        assertNotNull(otp.getOtp());
    }

    @Test
    void sendOtp_ShouldDelegateToService() {
        authService.sendOtp("user@demo.com");

        verify(userOtpService).sendOtp("user@demo.com");
    }

    @Test
    void refreshToken_ShouldThrow_WhenTokenInvalid() {
        when(refreshTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.refreshToken("bad"));
    }

    @Test
    void refreshToken_ShouldThrow_WhenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setToken("tok");
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThrows(RuntimeException.class, () -> authService.refreshToken("tok"));
    }

    @Test
    void refreshToken_ShouldIssueNewTokens_WhenValid() {
        RefreshToken token = new RefreshToken();
        token.setToken("tok");
        token.setUserId(3L);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        User user = buildUser();
        user.setUserId(3L);
        user.setEmail("user@demo.com");

        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        when(authRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(new CustomUserDetails(user));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("new-access");

        LoginResponseDto response = authService.refreshToken("tok");

        assertEquals("new-access", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        verify(refreshTokenRepository).delete(token);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_ShouldThrow_WhenUserMissing() {
        RefreshToken token = new RefreshToken();
        token.setToken("tok");
        token.setUserId(3L);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        when(authRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.refreshToken("tok"));
    }

    @Test
    void logout_ShouldDeleteRefreshToken_WhenValid() {
        RefreshRequestDto request = new RefreshRequestDto();
        request.setRefreshToken("token");
        RefreshToken token = new RefreshToken();
        token.setToken("token");
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(token));

        authService.logout(request);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void logout_ShouldThrow_WhenTokenMissing() {
        RefreshRequestDto request = new RefreshRequestDto();
        request.setRefreshToken("missing");
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.logout(request));
    }

    private SignUpDto buildSignUp() {
        SignUpDto dto = new SignUpDto();
        dto.setFullName("Test User");
        dto.setEmail("user@demo.com");
        dto.setPassword("Pass@123");
        dto.setPhone("9876543210");
        dto.setRole(Role.DRIVER);
        return dto;
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@demo.com");
        user.setPassword("pass");
        user.setRole(Role.DRIVER);
        return user;
    }
}
