package com.parkease.vehicle_service.utils;

import com.parkease.vehicle_service.entity.CustomUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

public class SecurityUtils {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

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


    public static String getCurrentUserRole() {
        List<String> roles = getCurrentUserRoles();
        if (!roles.isEmpty()) {
            return roles.get(0).replace("ROLE_", "");
        }

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

    public static List<String> getCurrentUserRoles() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String rolesHeader = request.getHeader(ROLES_HEADER);
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                return Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .toList();
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();
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
