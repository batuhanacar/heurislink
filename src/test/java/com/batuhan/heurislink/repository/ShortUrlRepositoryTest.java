package com.batuhan.heurislink.repository;

import com.batuhan.heurislink.entity.ShortUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class ShortUrlRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Test
    void shouldSaveShortUrl() {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.google.com",
                "abc123"
        );

        ShortUrl savedShortUrl = shortUrlRepository.save(shortUrl);

        assertNotNull(savedShortUrl.getId());
        assertEquals(
                "https://www.google.com",
                savedShortUrl.getOriginalUrl()
        );
        assertEquals("abc123", savedShortUrl.getShortCode());
        assertNotNull(savedShortUrl.getCreatedAt());
    }

    @Test
    void shouldFindShortUrlByShortCode() {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.github.com",
                "git123"
        );

        shortUrlRepository.save(shortUrl);

        Optional<ShortUrl> result =
                shortUrlRepository.findByShortCode("git123");

        assertTrue(result.isPresent());
        assertEquals(
                "https://www.github.com",
                result.get().getOriginalUrl()
        );
    }

    @Test
    void shouldReturnEmptyWhenShortCodeDoesNotExist() {
        Optional<ShortUrl> result =
                shortUrlRepository.findByShortCode("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenShortCodeExists() {
        ShortUrl shortUrl = new ShortUrl(
                "https://spring.io",
                "spring"
        );

        shortUrlRepository.save(shortUrl);

        boolean exists =
                shortUrlRepository.existsByShortCode("spring");

        assertTrue(exists);
    }

    @Test
    void shouldReturnFalseWhenShortCodeDoesNotExist() {
        boolean exists =
                shortUrlRepository.existsByShortCode("unknown");

        assertFalse(exists);
    }
}