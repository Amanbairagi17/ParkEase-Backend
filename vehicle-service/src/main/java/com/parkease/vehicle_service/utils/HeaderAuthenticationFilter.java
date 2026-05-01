package com.parkease.vehicle_service.utils;

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

        String userIdHeader = request.getHeader("X-User-Id");
        String rolesHeader  = request.getHeader("X-User-Roles");

        log.info("User Role : " +userIdHeader);
        log.info("User Header : " +rolesHeader);
        // Skip if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            if (userIdHeader != null && rolesHeader != null) {

                try {
                    Long userId = Long.parseLong(userIdHeader);

                    List<SimpleGrantedAuthority> authorities =
                            Arrays.stream(rolesHeader.split(","))
                                    .map(String::trim)
                                    .map(SimpleGrantedAuthority::new)
                                    .toList();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception ex) {
                    // Optional: log error
                    System.out.println("Invalid header auth: " + ex.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
