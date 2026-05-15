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

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String USER_NAME_HEADER = "X-User-Name";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public RequestInterceptor requestInterceptor() {
        return (RequestTemplate template) -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Forward gateway headers to downstream services
                String userId = request.getHeader(USER_ID_HEADER);
                String roles = request.getHeader(ROLES_HEADER);
                String name = request.getHeader(USER_NAME_HEADER);
                String authorization = request.getHeader(AUTHORIZATION_HEADER);

                if (userId != null) {
                    template.header(USER_ID_HEADER, userId);
                    log.debug("Forwarding X-User-Id={} to downstream service", userId);
                }
                if (roles != null) {
                    template.header(ROLES_HEADER, roles);
                    log.debug("Forwarding X-User-Roles={} to downstream service", roles);
                }
                if (name != null) {
                    template.header(USER_NAME_HEADER, name);
                }
                if (authorization != null) {
                    template.header(AUTHORIZATION_HEADER, authorization);
                }
                log.debug("Feign outgoing headers. AuthorizationPresent={}, X-User-Id={}, X-User-Roles={}",
                        authorization != null, userId, roles);
            } else {
                log.warn("No request context available for Feign propagation");
            }
        };
    }
}
