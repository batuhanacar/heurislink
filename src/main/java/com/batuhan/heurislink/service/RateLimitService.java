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

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitProperties rateLimitProperties;

    public void checkRateLimit(String clientIp) {
        String key = "rate-limit:create-url:" + clientIp;

        Long requestCount = stringRedisTemplate
                .opsForValue()
                .increment(key);

        if (requestCount != null && requestCount == 1) {
            stringRedisTemplate.expire(
                    key,
                    Duration.ofSeconds(
                            rateLimitProperties.windowSeconds()
                    )
            );
        }

        if (
                requestCount != null
                        && requestCount
                        > rateLimitProperties.maxRequests()
        ) {
            throw new RateLimitExceededException();
        }
    }
}