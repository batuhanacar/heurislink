package com.batuhan.heurislink.service;

import com.batuhan.heurislink.config.RateLimitProperties;
import com.batuhan.heurislink.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String KEY_PREFIX = "rate-limit:create-url:";

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitProperties rateLimitProperties;


    public void checkRateLimit(String clientIp) {
        String key = KEY_PREFIX + clientIp;

        Long requestCount = stringRedisTemplate
                .opsForValue()
                .increment(key);

        if (requestCount == null) {
            throw new IllegalStateException(
                    "Could not increment rate-limit counter"
            );
        }

        if (requestCount == 1L) {
            stringRedisTemplate.expire(
                    key,
                    Duration.ofSeconds(
                            rateLimitProperties.windowSeconds()
                    )
            );
        }

        if (requestCount > rateLimitProperties.maxRequests()) {
            throw new RateLimitExceededException();
        }
    }
}