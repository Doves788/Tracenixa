package com.tracenixa.platform.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.kafka.model.KafkaEvents.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationEventPublisher localEventPublisher;

    // Optional Kafka template injected only when Spring Kafka auto-configures
    private final org.springframework.beans.factory.ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Value("${tracenixa.async.use-kafka:false}")
    private boolean useKafka;

    public static final String TOPIC_TRACES = "tracenixa.traces.received";
    public static final String TOPIC_REPLAYS = "tracenixa.replays.requested";
    public static final String TOPIC_EVALUATIONS = "tracenixa.evaluations.requested";
    public static final String TOPIC_TELEMETRY_DLQ = "tracenixa.telemetry.dlq";
    public static final String TOPIC_REPLAYS_DLQ = "tracenixa.replays.dlq";

    public void publishTraceReceived(String traceId) {
        TraceReceivedEvent event = TraceReceivedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();

        sendEvent(TOPIC_TRACES, traceId, event);
    }

    public void publishReplayRequested(String sessionId, String projectId, String sourceTraceId, String mode, String faultConfigJson) {
        ReplayRequestedEvent event = ReplayRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .replaySessionId(sessionId)
                .projectId(projectId)
                .sourceTraceId(sourceTraceId)
                .replayMode(mode)
                .faultConfigJson(faultConfigJson)
                .timestamp(Instant.now())
                .build();

        sendEvent(TOPIC_REPLAYS, sessionId, event);
    }

    public void publishEvaluationRequested(String runId, String projectId, String datasetId, String modelIdentifier) {
        EvaluationRequestedEvent event = EvaluationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .evaluationRunId(runId)
                .projectId(projectId)
                .datasetId(datasetId)
                .modelIdentifier(modelIdentifier)
                .timestamp(Instant.now())
                .build();

        sendEvent(TOPIC_EVALUATIONS, runId, event);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

            if (useKafka && kafkaTemplate != null) {
                log.info("Publishing async event to Kafka topic [{}] key [{}]", topic, key);
                kafkaTemplate.send(topic, key, json);
            } else {
                // Fallback to internal Spring application event bus for standalone/local development
                log.debug("Publishing async event locally for topic [{}] key [{}]", topic, key);
                localEventPublisher.publishEvent(event);
            }
        } catch (Exception e) {
            log.warn("Failed to publish event to Kafka, falling back to local bus: {}", e.getMessage());
            localEventPublisher.publishEvent(event);
        }
    }

    public void publishToDlq(String dlqTopic, String rawPayload, String errorMessage) {
        log.error("Routing poisoned message to Dead Letter Queue [{}]. Reason: {}", dlqTopic, errorMessage);
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (useKafka && kafkaTemplate != null) {
            kafkaTemplate.send(dlqTopic, UUID.randomUUID().toString(), rawPayload);
        }
    }
}
