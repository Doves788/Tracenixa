package com.tracenixa.platform.trace.service;

import com.tracenixa.platform.trace.dto.TraceDtos.*;
import com.tracenixa.platform.trace.model.Span;
import com.tracenixa.platform.trace.model.Trace;
import com.tracenixa.platform.trace.repository.SpanRepository;
import com.tracenixa.platform.trace.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TraceQueryService {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;

    @Transactional(readOnly = true)
    public Page<TraceSummaryResponse> getTraces(
            String projectId,
            String status,
            String serviceName,
            String search,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<Trace> tracePage = traceRepository.searchTraces(
                projectId,
                (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) ? status.toUpperCase() : null,
                (serviceName != null && !serviceName.isBlank() && !"ALL".equalsIgnoreCase(serviceName)) ? serviceName : null,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        return tracePage.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public TraceDetailResponse getTraceDetail(String projectId, String traceId) {
        Trace trace = traceRepository.findByProjectIdAndTraceId(projectId, traceId)
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + traceId + " in project " + projectId));

        List<Span> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) {
            return TraceDetailResponse.builder()
                    .trace(toSummary(trace))
                    .spanTree(Collections.emptyList())
                    .chronologicalSpans(Collections.emptyList())
                    .build();
        }

        long traceStartEpoch = trace.getStartTime().toEpochMilli();
        long traceTotalDuration = Math.max(1, trace.getDurationMs());

        // Map spans to DTOs with Gantt timeline calculations
        Map<String, SpanNodeDto> nodeMap = new LinkedHashMap<>();
        List<SpanNodeDto> chronologicalList = new ArrayList<>();

        for (Span s : spans) {
            long spanStart = s.getStartTime().toEpochMilli();
            long offsetMs = Math.max(0, spanStart - traceStartEpoch);
            double offsetPct = Math.min(100.0, (offsetMs * 100.0) / traceTotalDuration);
            double widthPct = Math.max(2.0, Math.min(100.0 - offsetPct, (s.getDurationMs() * 100.0) / traceTotalDuration));

            SpanNodeDto node = SpanNodeDto.builder()
                    .spanId(s.getSpanId())
                    .parentSpanId(s.getParentSpanId())
                    .operationName(s.getOperationName())
                    .spanKind(s.getSpanKind())
                    .serviceName(s.getServiceName())
                    .status(s.getStatus())
                    .statusMessage(s.getStatusMessage())
                    .startTime(s.getStartTime())
                    .endTime(s.getEndTime())
                    .durationMs(s.getDurationMs())
                    .offsetPercentage(Math.round(offsetPct * 10.0) / 10.0)
                    .widthPercentage(Math.round(widthPct * 10.0) / 10.0)
                    .modelName(s.getModelName())
                    .promptTokens(s.getPromptTokens())
                    .completionTokens(s.getCompletionTokens())
                    .totalTokens(s.getTotalTokens())
                    .inputPayload(s.getInputPayload())
                    .outputPayload(s.getOutputPayload())
                    .attributes(s.getAttributes())
                    .children(new ArrayList<>())
                    .build();

            nodeMap.put(s.getSpanId(), node);
            chronologicalList.add(node);
        }

        // Build hierarchical tree
        List<SpanNodeDto> rootNodes = new ArrayList<>();
        for (SpanNodeDto node : nodeMap.values()) {
            if (node.getParentSpanId() != null && nodeMap.containsKey(node.getParentSpanId())) {
                nodeMap.get(node.getParentSpanId()).getChildren().add(node);
            } else {
                rootNodes.add(node);
            }
        }

        return TraceDetailResponse.builder()
                .trace(toSummary(trace))
                .spanTree(rootNodes)
                .chronologicalSpans(chronologicalList)
                .build();
    }

    private TraceSummaryResponse toSummary(Trace t) {
        String inPreview = t.getInputPayload();
        if (inPreview != null && inPreview.length() > 120) {
            inPreview = inPreview.substring(0, 120) + "...";
        }
        String outPreview = t.getOutputPayload();
        if (outPreview != null && outPreview.length() > 120) {
            outPreview = outPreview.substring(0, 120) + "...";
        }

        return TraceSummaryResponse.builder()
                .id(t.getId())
                .traceId(t.getTraceId())
                .projectId(t.getProject().getId())
                .rootOperationName(t.getRootOperationName())
                .serviceName(t.getServiceName())
                .status(t.getStatus())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .durationMs(t.getDurationMs())
                .totalSpans(t.getTotalSpans())
                .modelName(t.getModelName())
                .promptTokens(t.getPromptTokens())
                .completionTokens(t.getCompletionTokens())
                .totalTokens(t.getTotalTokens())
                .estimatedCost(t.getEstimatedCost())
                .inputPreview(inPreview)
                .outputPreview(outPreview)
                .errorMessage(t.getErrorMessage())
                .build();
    }
}
