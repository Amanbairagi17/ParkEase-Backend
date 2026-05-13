package com.parkease.receipt_service.utils;

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

        if (principal instanceof String str) {
            return Long.parseLong(str);
        }

        if (principal instanceof Long id) {
            return id;
        }

        throw new RuntimeException("Invalid authentication principal");
    }
}
