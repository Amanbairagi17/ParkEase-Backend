package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.CustomUserDetails;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JWTServiceImplTest {

    @InjectMocks
    private JWTServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
    }

    @Test
    void generateToken_ShouldExtractUsername_WhenValid() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser("user@demo.com"));

        String token = jwtService.generateToken(userDetails);

        assertEquals("user@demo.com", jwtService.extractUserName(token));
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenUsernameMatches() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser("user@demo.com"));

        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.validateToken(token, userDetails));
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenUsernameMismatch() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser("user@demo.com"));
        CustomUserDetails other = new CustomUserDetails(buildUser("other@demo.com"));

        String token = jwtService.generateToken(userDetails);

        assertFalse(jwtService.validateToken(token, other));
    }

    @Test
    void validateToken_ShouldThrow_WhenTokenInvalid() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser("user@demo.com"));

        assertThrows(AccessDeniedException.class, () -> jwtService.validateToken("invalid", userDetails));
    }

    @Test
    void validateToken_ShouldThrow_WhenTokenExpired() {
        CustomUserDetails userDetails = new CustomUserDetails(buildUser("user@demo.com"));

        String expiredToken = createExpiredToken("user@demo.com");

        assertThrows(AccessDeniedException.class, () -> jwtService.validateToken(expiredToken, userDetails));
    }

    private User buildUser(String email) {
        User user = new User();
        user.setUserId(1L);
        user.setEmail(email);
        user.setRole(Role.DRIVER);
        return user;
    }

    private String createExpiredToken(String email) {
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .subject(email)
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .signWith(key)
                .compact();
    }
}
