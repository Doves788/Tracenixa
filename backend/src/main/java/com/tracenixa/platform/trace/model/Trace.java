package com.tracenixa.platform.trace.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "traces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trace {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "root_operation_name", nullable = false)
    private String rootOperationName;

    @Column(name = "service_name", nullable = false, length = 128)
    private String serviceName;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "OK";

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "total_spans")
    @Builder.Default
    private Integer totalSpans = 1;

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

    @Column(name = "estimated_cost", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "input_payload", columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
