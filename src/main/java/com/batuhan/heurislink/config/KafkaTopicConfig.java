package com.batuhan.heurislink.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String URL_CLICKS_TOPIC = "url-clicks";
    public static final String URL_CLICKS_DLT_TOPIC = "url-clicks-dlt";

    @Bean
    public NewTopic urlClicksTopic() {
        return TopicBuilder
                .name(URL_CLICKS_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic urlClicksDltTopic() {
        return TopicBuilder
                .name(URL_CLICKS_DLT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}