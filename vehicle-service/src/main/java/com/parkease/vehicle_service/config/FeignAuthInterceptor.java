package com.parkease.vehicle_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("No request context available for vehicle-service Feign propagation");
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader("Authorization");
        String userId = request.getHeader("X-User-Id");
        String roles = request.getHeader("X-User-Roles");
        String userName = request.getHeader("X-User-Name");

        if (authorization != null) {
            template.header("Authorization", authorization);
        }
        if (userId != null) {
            template.header("X-User-Id", userId);
        }
        if (roles != null) {
            template.header("X-User-Roles", roles);
        }
        if (userName != null) {
            template.header("X-User-Name", userName);
        }

        log.debug("Feign outgoing headers. AuthorizationPresent={}, X-User-Id={}, X-User-Roles={}",
                authorization != null, userId, roles);
    }
}
