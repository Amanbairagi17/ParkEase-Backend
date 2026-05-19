package com.parkease.api_gateway.filter;

import com.parkease.api_gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class RoleFilter extends AbstractGatewayFilterFactory<RoleFilter.Config> {

    private final JwtUtil jwtUtil;

    public RoleFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Data
    public static class Config {
        private List<String> roles;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            if (config.getRoles() != null
                    && config.getRoles().size() == 1
                    && config.getRoles().contains("PUBLIC")) {
                log.debug("Skipping auth for pure PUBLIC route");
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid token", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.extractAllClaims(token);
                Long userId = jwtUtil.extractUserId(token);
                List<String> userRoles = jwtUtil.extractRoles(token);

                if (userRoles == null || userRoles.isEmpty()) {
                    return onError(exchange, "No roles found", HttpStatus.FORBIDDEN);
                }

                boolean allowed = config.getRoles().stream()
                        .anyMatch(role -> userRoles.contains("ROLE_" + role));

                if (!allowed) {
                    log.warn("Role denied. required={}, actual={}", config.getRoles(), userRoles);
                    return onError(exchange, "Forbidden", HttpStatus.FORBIDDEN);
                }

                exchange = exchange.mutate()
                        .request(r -> r
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .header("X-User-Id", String.valueOf(userId))
                                .header("X-User-Roles", String.join(",", userRoles))
                        )
                        .build();

                log.info("Gateway role pass. userId={}, roles={}", userId, userRoles);

                return chain.filter(exchange);

            } catch (Exception e) {
                log.error("Role filter token validation failed: {}", e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange ex, String msg, HttpStatus status) {
        ex.getResponse().setStatusCode(status);
        ex.getResponse().getHeaders().add("Content-Type", "application/json");
        return ex.getResponse().writeWith(
                Mono.just(ex.getResponse().bufferFactory()
                        .wrap(("{\"error\":\"" + msg + "\"}").getBytes()))
        );
    }
}
