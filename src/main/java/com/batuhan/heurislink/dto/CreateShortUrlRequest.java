package com.batuhan.heurislink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateShortUrlRequest(

        @NotBlank(message = "URL cannot be blank")
        @Pattern(
                regexp = "^(http|https)://.*$",
                message = "URL must start with http:// or https://"
        )
        String originalUrl

) {
}