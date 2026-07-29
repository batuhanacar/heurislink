package com.batuhan.heurislink.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UrlClickEvent(
        UUID shortUrlId,
        LocalDateTime clickedAt
) {
}