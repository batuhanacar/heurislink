package com.batuhan.heurislink.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record CreateShortUrlRequest(

        @NotBlank(message = "URL cannot be blank")
        @Pattern(
                regexp = "^https?://.*$",
                message = "URL must start with http:// or https://"
        )
        String originalUrl,

        @Future(message = "Expiration date must be in the future")
        LocalDateTime expiresAt

) {
}