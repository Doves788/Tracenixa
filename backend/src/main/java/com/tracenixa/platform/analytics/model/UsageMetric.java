package com.tracenixa.platform.analytics.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "usage_metrics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "metric_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageMetric {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "total_traces")
    @Builder.Default
    private Integer totalTraces = 0;

    @Column(name = "total_tokens")
    @Builder.Default
    private Long totalTokens = 0L;

    @Column(name = "prompt_tokens")
    @Builder.Default
    private Long promptTokens = 0L;

    @Column(name = "completion_tokens")
    @Builder.Default
    private Long completionTokens = 0L;

    @Column(name = "estimated_cost", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @Column(name = "avg_latency_ms")
    @Builder.Default
    private Double avgLatencyMs = 0.0;

    @Column(name = "error_count")
    @Builder.Default
    private Integer errorCount = 0;
}
