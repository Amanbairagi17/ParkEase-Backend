package com.parkease.booking_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Forward gateway headers to downstream services
                String userId = request.getHeader("X-User-Id");
                String roles  = request.getHeader("X-User-Roles");
                String name   = request.getHeader("X-User-Name");

                if (userId != null) {
                    template.header("X-User-Id", userId);
                    log.debug("Forwarding X-User-Id={} to downstream service", userId);
                }
                if (roles != null) {
                    template.header("X-User-Roles", roles);
                    log.debug("Forwarding X-User-Roles={} to downstream service", roles);
                }
                if (name != null) {
                    template.header("X-User-Name", name);
                }
            }
        };
    }
}