package com.batuhan.heurislink.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateShortUrlResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        LocalDateTime createdAt
) {
}