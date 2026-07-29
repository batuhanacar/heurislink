package com.batuhan.heurislink.dto;

import java.util.UUID;

public record UpdateShortUrlStatusResponse(
        UUID id,
        String shortCode,
        boolean active
) {
}