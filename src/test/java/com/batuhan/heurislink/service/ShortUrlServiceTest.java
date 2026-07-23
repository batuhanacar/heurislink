package com.batuhan.heurislink.service;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.ShortUrlNotFoundException;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    private ShortUrlService shortUrlService;

    @BeforeEach
    void setUp() {
        shortUrlService = new ShortUrlService(shortUrlRepository);
    }

    @Test
    void shouldCreateShortUrl() {
        String originalUrl = "https://www.google.com";

        when(shortUrlRepository.existsByShortCode(anyString()))
                .thenReturn(false);

        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrl result = shortUrlService.createShortUrl(originalUrl);

        assertNotNull(result);
        assertEquals(originalUrl, result.getOriginalUrl());
        assertNotNull(result.getShortCode());
        assertEquals(6, result.getShortCode().length());
        assertNotNull(result.getCreatedAt());

        verify(shortUrlRepository).save(any(ShortUrl.class));
    }

    @Test
    void shouldGenerateAnotherCodeWhenCodeAlreadyExists() {
        when(shortUrlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        when(shortUrlRepository.save(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        shortUrlService.createShortUrl("https://www.google.com");

        verify(shortUrlRepository, times(2))
                .existsByShortCode(anyString());
    }

    @Test
    void shouldReturnShortUrlWhenCodeExists() {
        ShortUrl shortUrl =
                new ShortUrl("https://www.google.com", "abc123");

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        ShortUrl result = shortUrlService.getByShortCode("abc123");

        assertEquals(shortUrl, result);
        assertEquals("https://www.google.com", result.getOriginalUrl());
    }

    @Test
    void shouldThrowExceptionWhenCodeDoesNotExist() {
        when(shortUrlRepository.findByShortCode("unknown"))
                .thenReturn(Optional.empty());

        ShortUrlNotFoundException exception = assertThrows(
                ShortUrlNotFoundException.class,
                () -> shortUrlService.getByShortCode("unknown")
        );

        assertEquals(
                "Short URL not found for code: unknown",
                exception.getMessage()
        );
    }
}