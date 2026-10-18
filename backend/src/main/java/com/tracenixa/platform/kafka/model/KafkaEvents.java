package com.tracenixa.platform.kafka.model;

import lombok.*;

import java.time.Instant;

public class KafkaEvents {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceReceivedEvent {
        private String eventId;
        private String traceId;
        private Instant timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplayRequestedEvent {
        private String eventId;
        private String replaySessionId;
        private String projectId;
        private String sourceTraceId;
        private String replayMode;
        private String faultConfigJson;
        private Instant timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationRequestedEvent {
        private String eventId;
        private String evaluationRunId;
        private String projectId;
        private String datasetId;
        private String modelIdentifier;
        private Instant timestamp;
    }
}
