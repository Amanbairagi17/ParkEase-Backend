package com.parkease.booking_service.utils;

import com.parkease.booking_service.entity.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authentication found");
        }

        Object principal = authentication.getPrincipal();

        // If you stored custom user object
        if (principal instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        // If principal is just userId (String or Long)
        if (principal instanceof String str) {
            return Long.parseLong(str);
        }

        if (principal instanceof Long id) {
            return id;
        }

        throw new RuntimeException("Invalid authentication principal");
    }
}