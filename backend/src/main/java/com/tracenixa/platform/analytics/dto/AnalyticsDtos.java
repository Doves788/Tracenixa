package com.tracenixa.platform.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AnalyticsDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewStatsDto {
        private long totalTraces;
        private double successRate;
        private long errorCount;
        private long p50LatencyMs;
        private long p95LatencyMs;
        private double avgLatencyMs;
        private long totalTokens;
        private long promptTokens;
        private long completionTokens;
        private BigDecimal estimatedCostUsd;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelCostBreakdownDto {
        private String modelName;
        private long totalTokens;
        private long executionCount;
        private BigDecimal estimatedCostUsd;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolPerformanceDto {
        private String toolName;
        private long callCount;
        private double avgDurationMs;
        private long failureCount;
        private double failureRatePct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LatencyBucketDto {
        private String label; // e.g. "0-200ms", "200-500ms", "500-1000ms", "1000-2000ms", ">2000ms"
        private int count;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrendDto {
        private String date;
        private int executions;
        private int errors;
        private long tokens;
        private double cost;
        private double avgLatencyMs;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentFailureDto {
        private String traceId;
        private String rootOperation;
        private String serviceName;
        private String errorMessage;
        private Long durationMs;
        private Instant timestamp;
    }
}
