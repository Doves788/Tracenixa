package com.tracenixa.platform.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.replay.model.RecordedEvent;
import com.tracenixa.platform.replay.repository.RecordedEventRepository;
import com.tracenixa.platform.trace.model.Span;
import com.tracenixa.platform.trace.model.Trace;
import com.tracenixa.platform.trace.repository.SpanRepository;
import com.tracenixa.platform.trace.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceRecorder {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final RecordedEventRepository recordedEventRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tracenixa.pricing.default-prompt-rate-per-1k:0.005}")
    private double defaultPromptRate;

    @Value("${tracenixa.pricing.default-completion-rate-per-1k:0.015}")
    private double defaultCompletionRate;

    public record TraceContext(
            String traceId,
            String rootSpanId,
            String projectId,
            String serviceName,
            Instant startTime
    ) {}

    public TraceContext startTrace(String projectId, String rootOperation, String serviceName, String inputPayload) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String rootSpanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Trace trace = Trace.builder()
                .id(UUID.randomUUID().toString())
                .traceId(traceId)
                .project(project)
                .rootOperationName(rootOperation)
                .serviceName(serviceName != null ? serviceName : "customer-support-service")
                .status("OK")
                .startTime(now)
                .endTime(now)
                .durationMs(0L)
                .totalSpans(1)
                .inputPayload(inputPayload)
                .createdAt(now)
                .build();
        traceRepository.save(trace);

        Span rootSpan = Span.builder()
                .id(UUID.randomUUID().toString())
                .traceId(traceId)
                .spanId(rootSpanId)
                .parentSpanId(null)
                .project(project)
                .operationName(rootOperation)
                .spanKind("AGENT")
                .serviceName(trace.getServiceName())
                .status("OK")
                .startTime(now)
                .endTime(now)
                .durationMs(0L)
                .inputPayload(inputPayload)
                .createdAt(now)
                .build();
        spanRepository.save(rootSpan);

        return new TraceContext(traceId, rootSpanId, projectId, trace.getServiceName(), now);
    }

    @Transactional
    public Span recordModelCall(
            TraceContext ctx,
            String operationName,
            String modelName,
            int promptTokens,
            int completionTokens,
            Instant start,
            Instant end,
            String prompt,
            String response,
            String status,
            String errorMessage
    ) {
        long durationMs = Math.max(1, end.toEpochMilli() - start.toEpochMilli());
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Project project = projectRepository.getReferenceById(ctx.projectId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("gen_ai.system", "spring_ai");
        attributes.put("gen_ai.request.model", modelName);
        attributes.put("gen_ai.usage.prompt_tokens", promptTokens);
        attributes.put("gen_ai.usage.completion_tokens", completionTokens);
        attributes.put("gen_ai.usage.total_tokens", promptTokens + completionTokens);

        String attrJson = "{}";
        try {
            attrJson = objectMapper.writeValueAsString(attributes);
        } catch (Exception ignored) {}

        Span span = Span.builder()
                .id(UUID.randomUUID().toString())
                .traceId(ctx.traceId())
                .spanId(spanId)
                .parentSpanId(ctx.rootSpanId())
                .project(project)
                .operationName(operationName)
                .spanKind("MODEL")
                .serviceName(ctx.serviceName())
                .status(status != null ? status : "OK")
                .statusMessage(errorMessage)
                .startTime(start)
                .endTime(end)
                .durationMs(durationMs)
                .attributes(attrJson)
                .modelName(modelName)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .inputPayload(prompt)
                .outputPayload(response)
                .createdAt(Instant.now())
                .build();
        spanRepository.save(span);

        // Record event for replay
        RecordedEvent event = RecordedEvent.builder()
                .id(UUID.randomUUID().toString())
                .traceId(ctx.traceId())
                .spanId(spanId)
                .eventType("MODEL_CALL")
                .stepOrder(getNextStepOrder(ctx.traceId()))
                .operationName(operationName)
                .inputData(prompt)
                .outputData(response)
                .durationMs(durationMs)
                .error(errorMessage)
                .isMocked(false)
                .createdAt(Instant.now())
                .build();
        recordedEventRepository.save(event);

        return span;
    }

    @Transactional
    public Span recordToolExecution(
            TraceContext ctx,
            String toolName,
            String inputJson,
            String outputJson,
            Instant start,
            Instant end,
            String status,
            String errorMessage
    ) {
        long durationMs = Math.max(1, end.toEpochMilli() - start.toEpochMilli());
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Project project = projectRepository.getReferenceById(ctx.projectId());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tool.name", toolName);
        attributes.put("tool.execution.status", status);

        String attrJson = "{}";
        try {
            attrJson = objectMapper.writeValueAsString(attributes);
        } catch (Exception ignored) {}

        Span span = Span.builder()
                .id(UUID.randomUUID().toString())
                .traceId(ctx.traceId())
                .spanId(spanId)
                .parentSpanId(ctx.rootSpanId())
                .project(project)
                .operationName("tool." + toolName)
                .spanKind("TOOL")
                .serviceName(ctx.serviceName())
                .status(status != null ? status : "OK")
                .statusMessage(errorMessage)
                .startTime(start)
                .endTime(end)
                .durationMs(durationMs)
                .attributes(attrJson)
                .inputPayload(inputJson)
                .outputPayload(outputJson)
                .createdAt(Instant.now())
                .build();
        spanRepository.save(span);

        // Record event for replay
        RecordedEvent event = RecordedEvent.builder()
                .id(UUID.randomUUID().toString())
                .traceId(ctx.traceId())
                .spanId(spanId)
                .eventType("TOOL_CALL")
                .stepOrder(getNextStepOrder(ctx.traceId()))
                .operationName(toolName)
                .inputData(inputJson)
                .outputData(outputJson)
                .durationMs(durationMs)
                .error(errorMessage)
                .isMocked(false)
                .createdAt(Instant.now())
                .build();
        recordedEventRepository.save(event);

        return span;
    }

    @Transactional
    public void finishTrace(TraceContext ctx, String finalOutput, String overallStatus, String errorMessage) {
        Instant end = Instant.now();
        long totalDuration = Math.max(1, end.toEpochMilli() - ctx.startTime().toEpochMilli());

        Trace trace = traceRepository.findByTraceId(ctx.traceId()).orElse(null);
        if (trace == null) return;

        List<Span> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(ctx.traceId());
        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        String detectedModel = "gpt-4o";

        for (Span s : spans) {
            if ("MODEL".equals(s.getSpanKind())) {
                totalPromptTokens += s.getPromptTokens() != null ? s.getPromptTokens() : 0;
                totalCompletionTokens += s.getCompletionTokens() != null ? s.getCompletionTokens() : 0;
                if (s.getModelName() != null) {
                    detectedModel = s.getModelName();
                }
            }
        }

        double cost = (totalPromptTokens / 1000.0 * defaultPromptRate) + (totalCompletionTokens / 1000.0 * defaultCompletionRate);

        trace.setEndTime(end);
        trace.setDurationMs(totalDuration);
        trace.setStatus(overallStatus != null ? overallStatus : "OK");
        trace.setOutputPayload(finalOutput);
        trace.setErrorMessage(errorMessage);
        trace.setTotalSpans(spans.size());
        trace.setModelName(detectedModel);
        trace.setPromptTokens(totalPromptTokens);
        trace.setCompletionTokens(totalCompletionTokens);
        trace.setTotalTokens(totalPromptTokens + totalCompletionTokens);
        trace.setEstimatedCost(BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP));
        traceRepository.save(trace);

        // Also update root span duration & output
        spanRepository.findBySpanId(ctx.rootSpanId()).ifPresent(rootSpan -> {
            rootSpan.setEndTime(end);
            rootSpan.setDurationMs(totalDuration);
            rootSpan.setStatus(overallStatus != null ? overallStatus : "OK");
            rootSpan.setStatusMessage(errorMessage);
            rootSpan.setOutputPayload(finalOutput);
            spanRepository.save(rootSpan);
        });
    }

    private int getNextStepOrder(String traceId) {
        List<RecordedEvent> events = recordedEventRepository.findByTraceIdOrderByStepOrderAsc(traceId);
        return events.size() + 1;
    }
}
