package com.batuhan.heurislink.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateShortUrlResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        LocalDateTime expiresAt,
        boolean active
) {
}