package com.batuhan.heurislink.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShortUrlAnalyticsResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        long clickCount,
        LocalDateTime createdAt
) {
}