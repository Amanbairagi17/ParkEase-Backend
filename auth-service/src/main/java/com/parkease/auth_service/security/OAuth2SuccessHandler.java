package com.parkease.auth_service.security;

import com.parkease.auth_service.entity.CustomUserDetails;
import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.repository.RefreshTokenRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.JWTService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${oauth2.redirect-uri}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = authToken.getPrincipal();

        String email   = oAuth2User.getAttribute("email");
        String name    = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String sub     = oAuth2User.getAttribute("sub");

        log.info("OAuth2 login: email={}", email);

        // Find or create user (safe + clean)
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Registering new OAuth user: {}", email);

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setProfilePicUrl(picture);
            newUser.setProvider("GOOGLE");
            newUser.setProviderId(sub);
            newUser.setRole(Role.DRIVER); // default role
            newUser.setIsActive(true);
            newUser.setCreatedAt(LocalDateTime.now());

            return userRepository.save(newUser);
        });

        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Generate tokens (role must already be inside JWT claims)
        String accessToken = jwtService.generateToken(userDetails);

        // Refresh token logic (same as normal login)
        refreshTokenRepository.deleteByUserId(user.getUserId());

        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getUserId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        refreshTokenRepository.save(refreshToken);

        log.info("OAuth success → userId={}, role={}", user.getUserId(), user.getRole());

        // Redirect URL (frontend will store tokens)
        String redirectUrl = buildRedirectUrl(user, accessToken, refreshTokenValue);

        log.info("Redirecting to: {}", redirectUrl);

        response.sendRedirect(redirectUrl);
    }

    // Clean URL builder (avoids messy string concat)
    private String buildRedirectUrl(User user, String accessToken, String refreshToken) {

        String baseUrl = getDashboardUrl(user.getRole());

        return baseUrl +
                "?accessToken=" + accessToken +
                "&refreshToken=" + refreshToken +
                "&role=" + user.getRole().name();
    }

    // Role-based frontend routing
    private String getDashboardUrl(Role role) {
        return "http://localhost:4200/oauth-success";
    }
}
