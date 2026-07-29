package com.batuhan.heurislink.messaging;

import com.batuhan.heurislink.event.UrlClickEvent;
import com.batuhan.heurislink.repository.UrlClickRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlClickConsumerTest {

    @Mock
    private UrlClickRepository urlClickRepository;

    @InjectMocks
    private UrlClickConsumer urlClickConsumer;

    @Test
    void shouldPersistClickEvent() {
        UUID eventId = UUID.randomUUID();
        UUID shortUrlId = UUID.randomUUID();
        LocalDateTime clickedAt = LocalDateTime.now();

        UrlClickEvent event =
                new UrlClickEvent(
                        eventId,
                        shortUrlId,
                        clickedAt
                );

        when(urlClickRepository.insertIfAbsent(
                any(UUID.class),
                eq(eventId),
                eq(shortUrlId),
                eq(clickedAt)
        )).thenReturn(1);

        urlClickConsumer.consume(event);

        verify(urlClickRepository).insertIfAbsent(
                any(UUID.class),
                eq(eventId),
                eq(shortUrlId),
                eq(clickedAt)
        );
    }

    @Test
    void shouldIgnoreDuplicateClickEvent() {
        UUID eventId = UUID.randomUUID();
        UUID shortUrlId = UUID.randomUUID();
        LocalDateTime clickedAt = LocalDateTime.now();

        UrlClickEvent event =
                new UrlClickEvent(
                        eventId,
                        shortUrlId,
                        clickedAt
                );

        when(urlClickRepository.insertIfAbsent(
                any(UUID.class),
                eq(eventId),
                eq(shortUrlId),
                eq(clickedAt)
        )).thenReturn(0);

        urlClickConsumer.consume(event);

        verify(urlClickRepository, times(1))
                .insertIfAbsent(
                        any(UUID.class),
                        eq(eventId),
                        eq(shortUrlId),
                        eq(clickedAt)
                );

        verifyNoMoreInteractions(urlClickRepository);
    }
}