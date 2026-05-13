package com.parkease.receipt_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Value("${receipt.service-auth.user-id:0}")
    private String serviceUserId;

    @Value("${receipt.service-auth.roles:ROLE_ADMIN}")
    private String serviceRoles;

    @Override
    public void apply(RequestTemplate template) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            Object principal = auth.getPrincipal();
            String userIdStr = null;

            if (principal instanceof Long longId) {
                userIdStr = String.valueOf(longId);
            } else if (principal instanceof String strId) {
                userIdStr = strId;
            }

            String roles = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            if (userIdStr != null) {
                template.header("X-User-Id", userIdStr);
                template.header("X-User-Roles", roles);
            }
            return;
        }

        template.header("X-User-Id", serviceUserId);
        template.header("X-User-Roles", serviceRoles);
        log.debug("Feign fallback headers: X-User-Id={}, X-User-Roles={}", serviceUserId, serviceRoles);
    }
}
