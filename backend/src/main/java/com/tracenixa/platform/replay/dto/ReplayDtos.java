package com.tracenixa.platform.replay.dto;

import com.tracenixa.platform.fault.model.FaultConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.List;

public class ReplayDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReplayRequest {
        @NotBlank(message = "sourceTraceId is required")
        private String sourceTraceId;

        @NotBlank(message = "replayMode is required: RECORDED, LIVE, or FAULT_INJECTION")
        private String replayMode;

        private FaultConfig faultConfig;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplaySessionResponse {
        private String id;
        private String projectId;
        private String sourceTraceId;
        private String replayedTraceId;
        private String replayMode;
        private String status;
        private Long originalDurationMs;
        private Long replayDurationMs;
        private Integer stepsCount;
        private Double similarityScore;
        private String divergenceSeverity;
        private String rootCauseCategory;
        private String diffSummary;
        private String errorMessage;
        private Instant createdAt;
        private Instant completedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DivergenceAnalysis {
        private boolean diverged;
        private double similarityScore; // 0.0 to 1.0 (e.g. 1.0 = 100% path parity)
        private String severity; // NONE, LOW, MEDIUM, CRITICAL
        private Integer firstDivergenceStep;
        private String rootCauseCategory; // NONE, INJECTED_FAULT, TOOL_SEQUENCE_MISMATCH, PARAMETER_DRIFT, OUTCOME_DRIFT, TIMING_DRIFT
        private String explanation;
        private List<String> sequenceDifferences;
        private int matchedSteps;
        private int divergedSteps;
        private int totalSteps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplayStepComparison {
        private int stepOrder;
        private String operationName;
        private String eventType;
        private String originalInput;
        private String originalOutput;
        private String replayedInput;
        private String replayedOutput;
        private Long originalDurationMs;
        private Long replayedDurationMs;
        private boolean match;
        private String status; // MATCHED, DIVERGED, INJECTED_FAULT, FAILED
        private String diffNote;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplayDetailResponse {
        private ReplaySessionResponse session;
        private DivergenceAnalysis divergenceAnalysis;
        private List<ReplayStepComparison> stepComparisons;
    }
}
