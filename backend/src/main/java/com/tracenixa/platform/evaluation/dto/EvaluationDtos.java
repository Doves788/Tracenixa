package com.tracenixa.platform.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class EvaluationDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseDto {
        private String id;
        private String name;
        private String input;
        private List<String> expectedTools;
        private List<String> requiredKeywords;
        private String expectedResolution; // REFUND, STATUS_LOOKUP, REPLACEMENT
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDatasetRequest {
        @NotBlank(message = "Dataset name is required")
        private String name;
        private String description;
        private String version;
        private List<TestCaseDto> testCases;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatasetResponse {
        private String id;
        private String projectId;
        private String name;
        private String description;
        private String version;
        private List<TestCaseDto> testCases;
        private int testCaseCount;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TriggerEvaluationRequest {
        @NotBlank(message = "datasetId is required")
        private String datasetId;
        private String name;
        private String agentVersion;
        private String modelIdentifier;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseResultDto {
        private String testCaseId;
        private String testCaseName;
        private String input;
        private String output;
        private List<String> toolsInvoked;
        private boolean toolsMatched;
        private boolean assertionsPassed;
        private double deterministicScore;
        private double faithfulnessScore;
        private double relevanceScore;
        private String status; // PASSED, FAILED
        private String failureReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationRunResponse {
        private String id;
        private String projectId;
        private String datasetId;
        private String datasetName;
        private String name;
        private String agentVersion;
        private String modelIdentifier;
        private String status;
        private Integer totalCases;
        private Integer passedCases;
        private Integer failedCases;
        private BigDecimal averageScore;
        private BigDecimal faithfulnessScore;
        private BigDecimal relevanceScore;
        private BigDecimal toolAccuracyScore;
        private List<TestCaseResultDto> results;
        private Instant createdAt;
        private Instant completedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CiCheckRequest {
        @NotBlank(message = "projectId is required")
        private String projectId;
        private String datasetId;
        @Builder.Default
        private double minPassRate = 80.0;
        @Builder.Default
        private double minFaithfulness = 75.0;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CiCheckResponse {
        private boolean passed;
        private int exitCode; // 0 = success, 1 = failure
        private String message;
        private double actualPassRate;
        private double actualFaithfulness;
        private String runId;
        private Instant evaluatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRegressionTestRequest {
        @NotBlank(message = "name is required")
        private String name;
        private String description;
        private String sourceTraceId;
        @NotBlank(message = "inputPayload is required")
        private String inputPayload;
        private List<String> expectedTools;
        private List<String> expectedAssertions;
        private Long toleranceMs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegressionTestResponse {
        private String id;
        private String projectId;
        private String name;
        private String description;
        private String sourceTraceId;
        private String inputPayload;
        private List<String> expectedTools;
        private List<String> expectedAssertions;
        private Long toleranceMs;
        private String status;
        private Instant lastRunAt;
        private String lastRunStatus;
        private Instant createdAt;
    }
}
