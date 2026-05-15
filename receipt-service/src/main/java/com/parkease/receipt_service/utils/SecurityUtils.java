package com.parkease.receipt_service.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SecurityUtils {

    private static final String USER_ID_HEADER = "X-User-Id";

    public static Long getCurrentUserId() {
        Long headerUserId = extractUserIdFromRequest();
        if (headerUserId != null) {
            return headerUserId;
        }

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authentication found");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String str) {
            return Long.parseLong(str);
        }

        if (principal instanceof Long id) {
            return id;
        }

        throw new RuntimeException("Invalid authentication principal");
    }

    public static boolean isCurrentUser(Long userId) {
        return userId != null && userId.equals(getCurrentUserId());
    }

    private static Long extractUserIdFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
