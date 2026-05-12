package com.parkease.api_gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate redisTemplate;

    private static final int LIMIT = 100;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    public boolean isAllowed(String ip) {

        String key = "rate:ip:" + ip;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount.equals(1L)) {
            redisTemplate.expire(key, WINDOW);
            return true;
        }

        if (currentCount <= LIMIT) {
            return true;
        }

        return false;
    }
}