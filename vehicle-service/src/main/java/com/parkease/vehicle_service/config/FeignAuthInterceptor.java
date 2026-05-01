package com.parkease.vehicle_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {

            // Get userId from principal
            Object principal = auth.getPrincipal();

            // Get roles
            String roles = auth.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            System.out.println("X-USER-Id : " +principal);
            System.out.println("X-USER-Roles : " +roles);
            if (principal != null) {
                template.header("X-User-Id", principal.toString());
                template.header("X-User-Roles", roles);
            }
        }
    }
}