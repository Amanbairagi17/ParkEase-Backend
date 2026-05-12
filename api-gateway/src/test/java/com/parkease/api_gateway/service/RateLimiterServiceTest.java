package com.parkease.api_gateway.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate redisTemplate;
    @Mock
    private ValueOperations valueOperations;

    @InjectMocks
    private RateLimiterService service;

    @Test
    void isAllowed_ShouldSetExpiry_WhenFirstRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean allowed = service.isAllowed("127.0.0.1");

        assertTrue(allowed);
        verify(redisTemplate).expire(anyString(), any(Duration.class));
    }

    @Test
    void isAllowed_ShouldAllow_WhenWithinLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(100L);

        boolean allowed = service.isAllowed("127.0.0.1");

        assertTrue(allowed);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void isAllowed_ShouldDeny_WhenLimitExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(101L);

        boolean allowed = service.isAllowed("127.0.0.1");

        assertFalse(allowed);
    }
}
