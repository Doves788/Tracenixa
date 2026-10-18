package com.tracenixa.platform.trace.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class TraceDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceSummaryResponse {
        private String id;
        private String traceId;
        private String projectId;
        private String rootOperationName;
        private String serviceName;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private Long durationMs;
        private Integer totalSpans;
        private String modelName;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private BigDecimal estimatedCost;
        private String inputPreview;
        private String outputPreview;
        private String errorMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpanNodeDto {
        private String spanId;
        private String parentSpanId;
        private String operationName;
        private String spanKind;
        private String serviceName;
        private String status;
        private String statusMessage;
        private Instant startTime;
        private Instant endTime;
        private Long durationMs;
        private Double offsetPercentage; // Gantt timeline position
        private Double widthPercentage;  // Gantt timeline duration
        private String modelName;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private String inputPayload;
        private String outputPayload;
        private String attributes;
        private List<SpanNodeDto> children;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TraceDetailResponse {
        private TraceSummaryResponse trace;
        private List<SpanNodeDto> spanTree;
        private List<SpanNodeDto> chronologicalSpans;
    }
}
