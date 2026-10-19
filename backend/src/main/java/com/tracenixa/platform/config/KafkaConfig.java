package com.tracenixa.platform.config;

import com.tracenixa.platform.kafka.producer.EventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "tracenixa.async.use-kafka", havingValue = "true")
public class KafkaConfig {

    @Bean
    public NewTopic tracesTopic() {
        return TopicBuilder.name(EventPublisher.TOPIC_TRACES)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic replaysTopic() {
        return TopicBuilder.name(EventPublisher.TOPIC_REPLAYS)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic evaluationsTopic() {
        return TopicBuilder.name(EventPublisher.TOPIC_EVALUATIONS)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic telemetryDlqTopic() {
        return TopicBuilder.name(EventPublisher.TOPIC_TELEMETRY_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic replaysDlqTopic() {
        return TopicBuilder.name(EventPublisher.TOPIC_REPLAYS_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
