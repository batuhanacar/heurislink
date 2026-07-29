package com.batuhan.heurislink.service;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.ShortUrlExpiredException;
import com.batuhan.heurislink.exception.ShortUrlNotFoundException;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
        assertEquals(10, result.getShortCode().length());
        assertNotNull(result.getShortCode());
        assertEquals(10, result.getShortCode().length());
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
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        ShortUrl result =
                shortUrlService.getByShortCode("abc123");

        assertEquals(shortUrl, result);
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

    @Test
    void shouldThrowExceptionWhenShortUrlIsExpired() {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "expired123",
                LocalDateTime.now().minusDays(1)
        );

        when(shortUrlRepository.findByShortCode("expired123"))
                .thenReturn(Optional.of(shortUrl));

        assertThrows(
                ShortUrlExpiredException.class,
                () -> shortUrlService.getByShortCode("expired123")
        );
    }

    @Test
    void shouldReturnShortUrlWhenExpirationDateIsInFuture() {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "future123",
                LocalDateTime.now().plusDays(1)
        );

        when(shortUrlRepository.findByShortCode("future123"))
                .thenReturn(Optional.of(shortUrl));

        ShortUrl result =
                shortUrlService.getByShortCode("future123");

        assertEquals(shortUrl, result);
    }

    @Test
    void shouldDeactivateShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        when(shortUrlRepository.save(shortUrl))
                .thenReturn(shortUrl);

        ShortUrl result =
                shortUrlService.deactivateShortUrl("abc123");

        assertFalse(result.isActive());

        verify(shortUrlRepository).save(shortUrl);
    }

    @Test
    void shouldActivateShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        shortUrl.deactivate();

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        when(shortUrlRepository.save(shortUrl))
                .thenReturn(shortUrl);

        ShortUrl result =
                shortUrlService.activateShortUrl("abc123");

        assertTrue(result.isActive());

        verify(shortUrlRepository).save(shortUrl);
    }

    @Test
    void shouldUpdateShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "https://old-example.com",
                "abc123"
        );

        LocalDateTime newExpiresAt =
                LocalDateTime.now().plusDays(7);

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        when(shortUrlRepository.save(shortUrl))
                .thenReturn(shortUrl);

        ShortUrl result = shortUrlService.updateShortUrl(
                "abc123",
                "https://new-example.com",
                newExpiresAt
        );

        assertEquals(
                "https://new-example.com",
                result.getOriginalUrl()
        );

        assertEquals(
                newExpiresAt,
                result.getExpiresAt()
        );

        verify(shortUrlRepository).save(shortUrl);
    }

    @Test
    void shouldDeleteShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        when(shortUrlRepository.findByShortCode("abc123"))
                .thenReturn(Optional.of(shortUrl));

        shortUrlService.deleteShortUrl("abc123");

        verify(shortUrlRepository).delete(shortUrl);
    }

    @Test
    void shouldThrowExceptionWhenDeletingUnknownShortUrl() {
        when(shortUrlRepository.findByShortCode("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> shortUrlService.deleteShortUrl("unknown")
        );
    }
}