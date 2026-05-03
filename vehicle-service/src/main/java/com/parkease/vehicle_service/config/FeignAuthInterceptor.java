package com.parkease.vehicle_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() != null) {

            // Extract userId as Long and convert to String for header
            Object principal = auth.getPrincipal();
            String userIdStr = null;

            if (principal instanceof Long longId) {
                userIdStr = String.valueOf(longId);
            } else if (principal instanceof String strId) {
                // Handle if somehow principal is still String
                try {
                    userIdStr = String.valueOf(Long.parseLong(strId));
                } catch (NumberFormatException e) {
                    log.warn("Invalid userId principal: {}", strId);
                    userIdStr = strId;
                }
            }

            // Get roles
            String roles = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            log.info("Feign outgoing headers: X-User-Id={}, X-User-Roles={}", userIdStr, roles);

            if (userIdStr != null) {
                template.header("X-User-Id", userIdStr);
                template.header("X-User-Roles", roles);
            }
        }
    }
}
