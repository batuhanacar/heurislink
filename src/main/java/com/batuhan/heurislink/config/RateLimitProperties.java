package com.batuhan.heurislink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        int maxRequests,
        long windowSeconds
) {
}