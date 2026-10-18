package com.tracenixa.platform.replay.model;

import com.tracenixa.platform.project.model.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "replay_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplaySession {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "source_trace_id", nullable = false, length = 64)
    private String sourceTraceId;

    @Column(name = "replayed_trace_id", length = 64)
    private String replayedTraceId;

    @Column(name = "replay_mode", nullable = false, length = 32)
    private String replayMode; // RECORDED, LIVE, FAULT_INJECTION

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED

    @Column(name = "fault_config", columnDefinition = "TEXT")
    private String faultConfig;

    @Column(name = "original_duration_ms")
    private Long originalDurationMs;

    @Column(name = "replay_duration_ms")
    private Long replayDurationMs;

    @Column(name = "steps_count")
    @Builder.Default
    private Integer stepsCount = 0;

    @Column(name = "diff_summary", columnDefinition = "TEXT")
    private String diffSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
