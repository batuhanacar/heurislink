package com.batuhan.heurislink.service;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.ShortUrlNotFoundException;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

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

    private String generateUniqueShortCode() {
        String shortCode = generateShortCode();

        while (shortUrlRepository.existsByShortCode(shortCode)) {
            shortCode = generateShortCode();
        }

        return shortCode;
    }

    public ShortUrl getByShortCode(String shortCode) {
        return shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new ShortUrlNotFoundException(shortCode)
                );
    }

    private String generateShortCode() {
        StringBuilder builder = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            builder.append(CHARACTERS.charAt(randomIndex));
        }

        return builder.toString();
    }

}
