package com.batuhan.heurislink.service;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.ShortUrlExpiredException;
import com.batuhan.heurislink.exception.ShortUrlInactiveException;
import com.batuhan.heurislink.exception.ShortUrlNotFoundException;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ShortUrlService {
    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int SHORT_CODE_LENGTH = 10;

    private final ShortUrlRepository shortUrlRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public ShortUrl createShortUrl(String originalUrl) {
        String shortCode = generateUniqueShortCode();

        ShortUrl shortUrl = new ShortUrl(originalUrl, shortCode);

        return shortUrlRepository.save(shortUrl);
    }

    public ShortUrl createShortUrl(
            String originalUrl,
            LocalDateTime expiresAt
    ) {
        String shortCode = generateUniqueShortCode();

        ShortUrl shortUrl =
                new ShortUrl(
                        originalUrl,
                        shortCode,
                        expiresAt
                );

        return shortUrlRepository.save(shortUrl);
    }

    private String generateUniqueShortCode() {
        String shortCode = generateShortCode();

        while (shortUrlRepository.existsByShortCode(shortCode)) {
            shortCode = generateShortCode();
        }

        return shortCode;
    }

    @Cacheable(value = "shortUrls", key = "#shortCode")
    public ShortUrl getByShortCode(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (!shortUrl.isActive()) {
            throw new ShortUrlInactiveException(shortCode);
        }

        LocalDateTime expiresAt = shortUrl.getExpiresAt();

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            throw new ShortUrlExpiredException(shortCode);
        }

        return shortUrl;
    }

    private String generateShortCode() {
        StringBuilder builder = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            builder.append(CHARACTERS.charAt(randomIndex));
        }

        return builder.toString();
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public ShortUrl deactivateShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        shortUrl.deactivate();

        return shortUrlRepository.save(shortUrl);
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public ShortUrl activateShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        shortUrl.activate();

        return shortUrlRepository.save(shortUrl);
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public ShortUrl updateShortUrl(
            String shortCode,
            String originalUrl,
            LocalDateTime expiresAt
    ) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        if (originalUrl != null) {
            shortUrl.updateOriginalUrl(originalUrl);
        }

        if (expiresAt != null) {
            shortUrl.updateExpiresAt(expiresAt);
        }

        return shortUrlRepository.save(shortUrl);
    }

    @Transactional
    @CacheEvict(value = "shortUrls", key = "#shortCode")
    public void deleteShortUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        shortUrlRepository.delete(shortUrl);
    }
}
