package com.parkease.payment_service.utils;

import com.parkease.payment_service.entity.CustomUserPrincipal;
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
            return userPrincipal.getId();
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


    public static String getCurrentUserRole() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("No authentication found");
        }

        Object principal = authentication.getPrincipal();

        //Case 1: CustomUserPrincipal
        if (principal instanceof CustomUserPrincipal userPrincipal) {
            return userPrincipal.getRole(); // make sure this method exists
        }

        // Case 2: Authorities from Spring Security
        if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
            return authentication.getAuthorities()
                    .iterator()
                    .next()
                    .getAuthority()
                    .replace("ROLE_", ""); // normalize
        }

        throw new RuntimeException("Unable to extract user role from authentication");
    }
}