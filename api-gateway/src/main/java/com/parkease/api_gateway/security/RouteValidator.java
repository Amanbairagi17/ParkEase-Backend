package com.parkease.api_gateway.security;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final List<String> openApiEndpoints = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify",
            "/api/auth/sendotp",
            "/api/auth/forget",
            "/api/v1/workspaces/public",
            "/api/v1/boards/get/workspace",
            "/api/v1/lists/public",
            "/oauth2/authorization/google",
            "/api/v1/subscriptions/details",
            "/login/oauth2/code/google",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    public Predicate<String> isSecured = uri -> {

        String path = uri.toLowerCase();

        for (String endpoint : openApiEndpoints) {
            if (path.equals(endpoint) || path.startsWith(endpoint + "/")) {
                return false; // public
            }
        }

        return true; // secured
    };
}