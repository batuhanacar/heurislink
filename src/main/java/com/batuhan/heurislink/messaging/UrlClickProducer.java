package com.batuhan.heurislink.messaging;

import com.batuhan.heurislink.config.KafkaTopicConfig;
import com.batuhan.heurislink.event.UrlClickEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlClickProducer {

    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    public void sendClickEvent(UrlClickEvent event) {
        kafkaTemplate
                .send(
                        KafkaTopicConfig.URL_CLICKS_TOPIC,
                        event.shortUrlId().toString(),
                        event
                )
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Failed to send click event for shortUrlId: {}",
                                event.shortUrlId(),
                                exception
                        );

                        return;
                    }

                    log.info(
                            "Click event sent successfully. topic: {}, partition: {}, offset: {}, shortUrlId: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.shortUrlId()
                    );
                });
    }
}