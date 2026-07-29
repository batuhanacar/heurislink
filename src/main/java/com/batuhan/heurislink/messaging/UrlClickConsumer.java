package com.batuhan.heurislink.messaging;

import com.batuhan.heurislink.entity.ShortUrl;
import com.batuhan.heurislink.entity.UrlClick;
import com.batuhan.heurislink.event.UrlClickEvent;
import com.batuhan.heurislink.repository.ShortUrlRepository;
import com.batuhan.heurislink.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UrlClickConsumer {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlClickRepository urlClickRepository;

    @KafkaListener(
            topics = "url-clicks",
            groupId = "heurislink-click-group"
    )
    @Transactional
    public void consume(UrlClickEvent event) {
        ShortUrl shortUrl = shortUrlRepository
                .findById(event.shortUrlId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Short URL not found: " + event.shortUrlId()
                        )
                );

        UrlClick urlClick =
                new UrlClick(shortUrl, event.clickedAt());

        urlClickRepository.save(urlClick);
    }
}