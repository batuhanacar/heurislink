package com.batuhan.heurislink.controller;

import com.batuhan.heurislink.dto.*;
import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.event.UrlClickEvent;
import com.batuhan.heurislink.messaging.UrlClickProducer;
import com.batuhan.heurislink.service.RateLimitService;
import com.batuhan.heurislink.service.ShortUrlService;
import com.batuhan.heurislink.service.UrlClickService;
import com.batuhan.heurislink.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final UrlClickService urlClickService;
    private final UrlClickProducer urlClickProducer;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.base-url}")
    private String baseUrl;

    @PostMapping("/urls")
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request,
            HttpServletRequest httpServletRequest) {

        String clientIp = clientIpResolver.resolve(httpServletRequest);
        rateLimitService.checkRateLimit(clientIp);

        ShortUrl shortUrl = shortUrlService.createShortUrl(
                request.originalUrl(),
                request.expiresAt()
        );

        String fullShortUrl =
                baseUrl + "/" + shortUrl.getShortCode();

        CreateShortUrlResponse response =
                new CreateShortUrlResponse(
                        shortUrl.getId(),
                        shortUrl.getOriginalUrl(),
                        shortUrl.getShortCode(),
                        fullShortUrl,
                        shortUrl.getCreatedAt(),
                        shortUrl.getExpiresAt(),
                        shortUrl.isActive()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl =
                shortUrlService.getByShortCode(shortCode);

        UrlClickEvent event =
                new UrlClickEvent(
                        UUID.randomUUID(),
                        shortUrl.getId(),
                        LocalDateTime.now()
                );

        urlClickProducer.sendClickEvent(event);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(shortUrl.getOriginalUrl()))
                .build();
    }

    @GetMapping("/urls/{shortCode}/analytics")
    public ResponseEntity<ShortUrlAnalyticsResponse> getAnalytics(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl =
                shortUrlService.getByShortCode(shortCode);

        long clickCount =
                urlClickService.getClickCount(shortUrl);

        ShortUrlAnalyticsResponse response =
                new ShortUrlAnalyticsResponse(
                        shortUrl.getId(),
                        shortUrl.getOriginalUrl(),
                        shortUrl.getShortCode(),
                        clickCount,
                        shortUrl.getCreatedAt()
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/urls/{shortCode}/deactivate")
    public ResponseEntity<UpdateShortUrlStatusResponse> deactivateShortUrl(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl =
                shortUrlService.deactivateShortUrl(shortCode);

        UpdateShortUrlStatusResponse response =
                new UpdateShortUrlStatusResponse(
                        shortUrl.getId(),
                        shortUrl.getShortCode(),
                        shortUrl.isActive()
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/urls/{shortCode}/activate")
    public ResponseEntity<UpdateShortUrlStatusResponse> activateShortUrl(
            @PathVariable String shortCode
    ) {
        ShortUrl shortUrl =
                shortUrlService.activateShortUrl(shortCode);

        UpdateShortUrlStatusResponse response =
                new UpdateShortUrlStatusResponse(
                        shortUrl.getId(),
                        shortUrl.getShortCode(),
                        shortUrl.isActive()
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/urls/{shortCode}")
    public ResponseEntity<UpdateShortUrlResponse> updateShortUrl(
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateShortUrlRequest request
    ) {
        ShortUrl shortUrl = shortUrlService.updateShortUrl(
                shortCode,
                request.originalUrl(),
                request.expiresAt()
        );

        UpdateShortUrlResponse response = new UpdateShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getOriginalUrl(),
                shortUrl.getShortCode(),
                shortUrl.getExpiresAt(),
                shortUrl.isActive()
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<Void> deleteShortUrl(
            @PathVariable String shortCode
    ) {
        shortUrlService.deleteShortUrl(shortCode);

        return ResponseEntity.noContent().build();
    }
}