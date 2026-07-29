package com.batuhan.heurislink.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record UpdateShortUrlRequest(

        @Pattern(
                regexp = "^https?://.*$",
                message = "URL must start with http:// or https://"
        )
        String originalUrl,

        @Future(message = "Expiration date must be in the future")
        LocalDateTime expiresAt

) {
}