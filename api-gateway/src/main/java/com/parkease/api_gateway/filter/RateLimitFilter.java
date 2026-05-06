package com.parkease.api_gateway.filter;

import com.parkease.api_gateway.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {
    private final RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        String ip = exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();

        boolean allowed = rateLimiterService.isAllowed(ip);

        if (!allowed) {
            exchange.getResponse()
                    .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }


    @Override
    public int getOrder() {
        return -10;
    }
}