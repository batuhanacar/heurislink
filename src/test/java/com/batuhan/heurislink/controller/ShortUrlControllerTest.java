package com.batuhan.heurislink.controller;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.exception.*;
import com.batuhan.heurislink.messaging.UrlClickProducer;
import com.batuhan.heurislink.service.RateLimitService;
import com.batuhan.heurislink.service.ShortUrlService;
import com.batuhan.heurislink.service.UrlClickService;
import com.batuhan.heurislink.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ShortUrlController.class,
        properties = "app.base-url=http://localhost:8080"
)
@Import(GlobalExceptionHandler.class)
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortUrlService shortUrlService;

    @MockitoBean
    private UrlClickService urlClickService;

    @MockitoBean
    private UrlClickProducer urlClickProducer;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        when(clientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenReturn("203.0.113.25");
    }

    @Test
    void shouldCreateShortUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.google.com",
                "abc123"
        );

        when(clientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenReturn("203.0.113.25");

        when(shortUrlService.createShortUrl(
                eq("https://www.google.com"),
                isNull()
        )).thenReturn(shortUrl);

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "originalUrl": "https://www.google.com"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.google.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value("abc123"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/abc123"));

        verify(clientIpResolver)
                .resolve(any(HttpServletRequest.class));

        verify(rateLimitService)
                .checkRateLimit("203.0.113.25");
    }

    @Test
    void shouldCreateShortUrlWithExpirationDate() throws Exception {
        LocalDateTime expiresAt =
                LocalDateTime.of(2030, 1, 1, 10, 0);

        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "exp123",
                expiresAt
        );

        when(shortUrlService.createShortUrl(
                eq("https://example.com"),
                eq(expiresAt)
        )).thenReturn(shortUrl);

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com",
                                  "expiresAt": "2030-01-01T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://example.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value("exp123"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/exp123"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2030-01-01T10:00:00"));
    }

    @Test
    void shouldReturnBadRequestWhenExpirationDateIsInThePast()
            throws Exception {

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com",
                                  "expiresAt": "2020-01-01T10:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Expiration date must be in the future"));
    }

    @Test
    void shouldRedirectToOriginalUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://www.google.com",
                "abc123"
        );

        when(shortUrlService.getByShortCode("abc123"))
                .thenReturn(shortUrl);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "https://www.google.com"
                ));
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsBlank() throws Exception {
        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturnBadRequestWhenUrlDoesNotContainProtocol()
            throws Exception {

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "www.google.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("URL must start with http:// or https://"));
    }

    @Test
    void shouldReturnNotFoundWhenShortCodeDoesNotExist()
            throws Exception {

        when(shortUrlService.getByShortCode("unknown"))
                .thenThrow(new ShortUrlNotFoundException("unknown"));

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL not found for code: unknown"));
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitIsExceeded()
            throws Exception {

        doThrow(new RateLimitExceededException())
                .when(rateLimitService)
                .checkRateLimit(anyString());

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error")
                        .value("Too Many Requests"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "Rate limit exceeded. Please try again later."
                        ));
    }

    @Test
    void shouldReturnGoneWhenShortUrlIsExpired() throws Exception {
        when(shortUrlService.getByShortCode("expired123"))
                .thenThrow(new ShortUrlExpiredException("expired123"));

        mockMvc.perform(get("/expired123"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL has expired for code: expired123"));
    }

    @Test
    void shouldReturnGoneWhenShortUrlIsInactive() throws Exception {
        when(shortUrlService.getByShortCode("inactive123"))
                .thenThrow(new ShortUrlInactiveException("inactive123"));

        mockMvc.perform(get("/inactive123"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message")
                        .value("Short URL is inactive for code: inactive123"));
    }

    @Test
    void shouldDeactivateShortUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        shortUrl.deactivate();

        when(shortUrlService.deactivateShortUrl("abc123"))
                .thenReturn(shortUrl);

        mockMvc.perform(patch("/urls/abc123/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode")
                        .value("abc123"))
                .andExpect(jsonPath("$.active")
                        .value(false));
    }

    @Test
    void shouldActivateShortUrl() throws Exception {
        ShortUrl shortUrl = new ShortUrl(
                "https://example.com",
                "abc123"
        );

        when(shortUrlService.activateShortUrl("abc123"))
                .thenReturn(shortUrl);

        mockMvc.perform(patch("/urls/abc123/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode")
                        .value("abc123"))
                .andExpect(jsonPath("$.active")
                        .value(true));
    }

    @Test
    void shouldUpdateShortUrl() throws Exception {
        LocalDateTime expiresAt =
                LocalDateTime.of(2030, 1, 1, 10, 0);

        ShortUrl shortUrl = new ShortUrl(
                "https://new-example.com",
                "abc123",
                expiresAt
        );

        when(shortUrlService.updateShortUrl(
                eq("abc123"),
                eq("https://new-example.com"),
                eq(expiresAt)
        )).thenReturn(shortUrl);

        mockMvc.perform(patch("/urls/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "originalUrl": "https://new-example.com",
                              "expiresAt": "2030-01-01T10:00:00"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://new-example.com"))
                .andExpect(jsonPath("$.shortCode")
                        .value("abc123"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2030-01-01T10:00:00"))
                .andExpect(jsonPath("$.active")
                        .value(true));
    }

    @Test
    void shouldDeleteShortUrl() throws Exception {
        mockMvc.perform(delete("/urls/abc123"))
                .andExpect(status().isNoContent());
    }
}