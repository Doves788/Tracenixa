package com.tracenixa.platform.replay.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "recorded_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordedEvent {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "span_id", length = 64)
    private String spanId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType; // MODEL_CALL, TOOL_CALL, TOOL_RESPONSE, AGENT_STATE

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "operation_name", nullable = false, length = 128)
    private String operationName;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(name = "is_mocked")
    @Builder.Default
    private Boolean isMocked = false;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
