package com.tracenixa.platform.evaluation.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "regression_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegressionTest {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_trace_id", length = 64)
    private String sourceTraceId;

    @Column(name = "input_payload", nullable = false, columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "expected_tools", columnDefinition = "TEXT")
    private String expectedTools; // JSON array of expected tool names

    @Column(name = "expected_assertions", columnDefinition = "TEXT")
    private String expectedAssertions; // JSON array of assertions

    @Column(name = "tolerance_ms")
    @Builder.Default
    private Long toleranceMs = 5000L;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, MUTED, FAILING

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_run_status", length = 32)
    private String lastRunStatus; // PASSED, FAILED

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
