package com.batuhan.heurislink.messaging;

import com.batuhan.heurislink.config.KafkaTopicConfig;
import com.batuhan.heurislink.event.UrlClickEvent;
import com.batuhan.heurislink.repository.UrlClickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlClickConsumer {

    private final UrlClickRepository urlClickRepository;

    @KafkaListener(
            topics = KafkaTopicConfig.URL_CLICKS_TOPIC,
            groupId = "heurislink-click-group"
    )
    @Transactional
    public void consume(UrlClickEvent event) {
        log.info(
                "Consuming click event. eventId: {}, shortUrlId: {}",
                event.eventId(),
                event.shortUrlId()
        );

        int insertedRows = urlClickRepository.insertIfAbsent(
                UUID.randomUUID(),
                event.eventId(),
                event.shortUrlId(),
                event.clickedAt()
        );

        if (insertedRows == 0) {
            log.warn(
                    "Duplicate click event ignored. eventId: {}",
                    event.eventId()
            );

            return;
        }

        log.info(
                "Click event persisted successfully. eventId: {}",
                event.eventId()
        );
    }
}