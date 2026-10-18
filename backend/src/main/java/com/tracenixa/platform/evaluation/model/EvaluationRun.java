package com.tracenixa.platform.evaluation.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "evaluation_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationRun {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private EvaluationDataset dataset;

    @Column(nullable = false)
    private String name;

    @Column(name = "agent_version", nullable = false, length = 64)
    @Builder.Default
    private String agentVersion = "v1.0";

    @Column(name = "model_identifier", nullable = false, length = 128)
    private String modelIdentifier;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "total_cases")
    @Builder.Default
    private Integer totalCases = 0;

    @Column(name = "passed_cases")
    @Builder.Default
    private Integer passedCases = 0;

    @Column(name = "failed_cases")
    @Builder.Default
    private Integer failedCases = 0;

    @Column(name = "average_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal averageScore = BigDecimal.ZERO;

    @Column(name = "faithfulness_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal faithfulnessScore = BigDecimal.ZERO;

    @Column(name = "relevance_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal relevanceScore = BigDecimal.ZERO;

    @Column(name = "tool_accuracy_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal toolAccuracyScore = BigDecimal.ZERO;

    @Column(name = "detailed_results", columnDefinition = "TEXT")
    private String detailedResults;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
