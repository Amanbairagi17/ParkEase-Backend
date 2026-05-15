package com.parkease.parkingspot_service.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String roles = request.getHeader("X-User-Roles");
        String authorization = request.getHeader("Authorization");

        log.debug("Incoming auth headers. AuthorizationPresent={}, X-User-Id={}, X-User-Roles={}",
                authorization != null, userId, roles);

        if (SecurityContextHolder.getContext().getAuthentication() == null
                && userId != null && !userId.isBlank()
                && roles != null && !roles.isBlank()) {
            try {
                List<SimpleGrantedAuthority> authorities =
                        Arrays.stream(roles.split(","))
                                .map(String::trim)
                                .filter(role -> !role.isBlank())
                                .map(SimpleGrantedAuthority::new)
                                .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                Long.parseLong(userId.trim()),
                                null,
                                authorities
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException ex) {
                log.warn("Invalid X-User-Id header: {}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
