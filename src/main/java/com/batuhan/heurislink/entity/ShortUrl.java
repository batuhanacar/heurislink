package com.batuhan.heurislink.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "short_urls")
public class ShortUrl implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "original_url",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String originalUrl;

    @Column(
            name = "short_code",
            nullable = false,
            unique = true,
            length = 10
    )
    private String shortCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    public ShortUrl(String originalUrl, String shortCode) {
        this(originalUrl, shortCode, null);
    }

    public ShortUrl(
            String originalUrl,
            String shortCode,
            LocalDateTime expiresAt
    ) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}