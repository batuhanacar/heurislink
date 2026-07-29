package com.batuhan.heurislink.service;

import com.batuhan.heurislink.config.RateLimitProperties;
import com.batuhan.heurislink.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RateLimitProperties rateLimitProperties;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue())
                .thenReturn(valueOperations);

        rateLimitService =
                new RateLimitService(
                        stringRedisTemplate,
                        rateLimitProperties
                );
    }

    @Test
    void shouldAllowRequestWhenLimitIsNotExceeded() {
        when(valueOperations.increment(anyString()))
                .thenReturn(5L);

        when(rateLimitProperties.maxRequests())
                .thenReturn(10);

        assertDoesNotThrow(() ->
                rateLimitService.checkRateLimit("127.0.0.1")
        );

        verify(valueOperations)
                .increment("rate-limit:create-url:127.0.0.1");

        verify(stringRedisTemplate, never())
                .expire(anyString(), any(Duration.class));
    }

    @Test
    void shouldSetExpirationForFirstRequest() {
        when(valueOperations.increment(anyString()))
                .thenReturn(1L);

        when(rateLimitProperties.maxRequests())
                .thenReturn(10);

        when(rateLimitProperties.windowSeconds())
                .thenReturn(60L);

        assertDoesNotThrow(() ->
                rateLimitService.checkRateLimit("127.0.0.1")
        );

        verify(stringRedisTemplate).expire(
                "rate-limit:create-url:127.0.0.1",
                Duration.ofSeconds(60)
        );
    }

    @Test
    void shouldRejectRequestWhenLimitIsExceeded() {
        when(valueOperations.increment(anyString()))
                .thenReturn(11L);

        when(rateLimitProperties.maxRequests())
                .thenReturn(10);

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitService.checkRateLimit("127.0.0.1")
        );
    }
}