package com.tracenixa.platform.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.evaluation.dto.EvaluationDtos.TriggerEvaluationRequest;
import com.tracenixa.platform.evaluation.service.EvaluationService;
import com.tracenixa.platform.kafka.model.KafkaEvents.*;
import com.tracenixa.platform.kafka.producer.EventPublisher;
import com.tracenixa.platform.replay.service.ReplayEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final ReplayEngineService replayEngineService;
    private final EvaluationService evaluationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory idempotency deduplication cache
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    // --- Kafka Topic Listeners ---

    @KafkaListener(topics = EventPublisher.TOPIC_TRACES, groupId = "tracenixa-workers", autoStartup = "${tracenixa.async.use-kafka:false}")
    public void onKafkaTraceReceived(String message) {
        try {
            TraceReceivedEvent event = objectMapper.readValue(message, TraceReceivedEvent.class);
            handleTraceReceived(event);
        } catch (Exception e) {
            log.error("Kafka error processing TraceReceivedEvent: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventPublisher.TOPIC_REPLAYS, groupId = "tracenixa-workers", autoStartup = "${tracenixa.async.use-kafka:false}")
    public void onKafkaReplayRequested(String message) {
        try {
            ReplayRequestedEvent event = objectMapper.readValue(message, ReplayRequestedEvent.class);
            handleReplayRequested(event);
        } catch (Exception e) {
            log.error("Kafka error processing ReplayRequestedEvent: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventPublisher.TOPIC_EVALUATIONS, groupId = "tracenixa-workers", autoStartup = "${tracenixa.async.use-kafka:false}")
    public void onKafkaEvaluationRequested(String message) {
        try {
            EvaluationRequestedEvent event = objectMapper.readValue(message, EvaluationRequestedEvent.class);
            handleEvaluationRequested(event);
        } catch (Exception e) {
            log.error("Kafka error processing EvaluationRequestedEvent: {}", e.getMessage(), e);
        }
    }

    // --- Local Spring Application Event Listeners (Dual Support for standalone local dev) ---

    @Async
    @EventListener
    public void onLocalTraceReceived(TraceReceivedEvent event) {
        handleTraceReceived(event);
    }

    @Async
    @EventListener
    public void onLocalReplayRequested(ReplayRequestedEvent event) {
        handleReplayRequested(event);
    }

    @Async
    @EventListener
    public void onLocalEvaluationRequested(EvaluationRequestedEvent event) {
        handleEvaluationRequested(event);
    }

    // --- Idempotent Worker Processing Logic ---

    private void handleTraceReceived(TraceReceivedEvent event) {
        if (event == null || !processedEventIds.add(event.getEventId())) {
            log.debug("Skipping duplicate or null trace event: {}", event != null ? event.getEventId() : null);
            return;
        }
        log.info("Worker processed TraceReceivedEvent for trace: {}", event.getTraceId());
    }

    private void handleReplayRequested(ReplayRequestedEvent event) {
        if (event == null || !processedEventIds.add(event.getEventId())) {
            return;
        }
        log.info("Worker starting async replay session: {}", event.getReplaySessionId());
        replayEngineService.executeReplaySession(event.getReplaySessionId());
    }

    private void handleEvaluationRequested(EvaluationRequestedEvent event) {
        if (event == null || !processedEventIds.add(event.getEventId())) {
            return;
        }
        log.info("Worker starting async evaluation run: {}", event.getEvaluationRunId());
        try {
            evaluationService.runEvaluation(event.getProjectId(), TriggerEvaluationRequest.builder()
                    .datasetId(event.getDatasetId())
                    .name("Async Evaluation Run " + event.getEvaluationRunId().substring(0, 8))
                    .agentVersion("v1.0")
                    .modelIdentifier(event.getModelIdentifier())
                    .build());
        } catch (Exception e) {
            log.error("Error executing async evaluation: {}", e.getMessage(), e);
        }
    }
}
