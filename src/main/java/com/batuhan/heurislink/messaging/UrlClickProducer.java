package com.batuhan.heurislink.messaging;

import com.batuhan.heurislink.event.UrlClickEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UrlClickProducer {

    private static final String TOPIC_NAME = "url-clicks";

    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    public void sendClickEvent(UrlClickEvent event) {
        kafkaTemplate.send(
                TOPIC_NAME,
                event.shortUrlId().toString(),
                event
        );
    }
}