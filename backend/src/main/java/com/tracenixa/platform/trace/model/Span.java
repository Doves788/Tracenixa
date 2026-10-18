package com.tracenixa.platform.trace.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "spans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Span {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "span_id", nullable = false, length = 64)
    private String spanId;

    @Column(name = "parent_span_id", length = 64)
    private String parentSpanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "operation_name", nullable = false)
    private String operationName;

    @Column(name = "span_kind", nullable = false, length = 32)
    @Builder.Default
    private String spanKind = "INTERNAL";

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "OK";

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Column(columnDefinition = "TEXT")
    private String events;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "prompt_tokens")
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens")
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "total_tokens")
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
