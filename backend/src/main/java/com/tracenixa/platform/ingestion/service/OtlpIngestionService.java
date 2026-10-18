package com.tracenixa.platform.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.kafka.producer.EventPublisher;
import com.tracenixa.platform.project.context.ProjectContext;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
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
public class OtlpIngestionService {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final ProjectRepository projectRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tracenixa.pricing.default-prompt-rate-per-1k:0.005}")
    private double defaultPromptRate;

    @Value("${tracenixa.pricing.default-completion-rate-per-1k:0.015}")
    private double defaultCompletionRate;

    @Transactional
    public Map<String, Object> ingestOtlpTraces(String rawJsonPayload, String headerProjectId) {
        try {
            JsonNode root = objectMapper.readTree(rawJsonPayload);
            JsonNode resourceSpans = root.get("resourceSpans");

            if (resourceSpans == null || !resourceSpans.isArray() || resourceSpans.isEmpty()) {
                throw new IllegalArgumentException("Invalid OTLP payload: missing 'resourceSpans' array");
            }

            int ingestedSpansCount = 0;
            Set<String> traceIdsAffected = new HashSet<>();

            for (JsonNode resourceSpan : resourceSpans) {
                // Extract resource attributes (service.name, project.id)
                String serviceName = "agent-service";
                String projectIdFromResource = null;

                JsonNode resource = resourceSpan.get("resource");
                if (resource != null && resource.has("attributes")) {
                    for (JsonNode attr : resource.get("attributes")) {
                        String key = attr.path("key").asText();
                        JsonNode val = attr.path("value");
                        if ("service.name".equals(key)) {
                            serviceName = extractValueAsString(val);
                        } else if ("project.id".equals(key)) {
                            projectIdFromResource = extractValueAsString(val);
                        }
                    }
                }

                // Determine target project
                String resolvedProjectId = headerProjectId != null ? headerProjectId :
                        (ProjectContext.getProjectId() != null ? ProjectContext.getProjectId() : projectIdFromResource);

                if (resolvedProjectId == null) {
                    resolvedProjectId = projectRepository.findAll().stream()
                            .findFirst().map(Project::getId)
                            .orElseThrow(() -> new IllegalStateException("No project available to associate incoming trace telemetry"));
                }
                final String targetProjectId = resolvedProjectId;

                Project project = projectRepository.findById(targetProjectId)
                        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + targetProjectId));

                JsonNode scopeSpans = resourceSpan.get("scopeSpans");
                if (scopeSpans == null) continue;

                for (JsonNode scopeSpan : scopeSpans) {
                    JsonNode spansNode = scopeSpan.get("spans");
                    if (spansNode == null) continue;

                    for (JsonNode sNode : spansNode) {
                        String traceId = sNode.path("traceId").asText();
                        String spanId = sNode.path("spanId").asText();
                        String parentSpanId = sNode.has("parentSpanId") && !sNode.path("parentSpanId").asText().isBlank()
                                ? sNode.path("parentSpanId").asText() : null;
                        String name = sNode.path("name").asText("operation");

                        long startNano = parseNano(sNode.path("startTimeUnixNano").asText("0"));
                        long endNano = parseNano(sNode.path("endTimeUnixNano").asText("0"));
                        if (endNano <= startNano) {
                            endNano = startNano + 50_000_000L; // default 50ms
                        }

                        Instant startInstant = Instant.ofEpochMilli(startNano / 1_000_000L);
                        Instant endInstant = Instant.ofEpochMilli(endNano / 1_000_000L);
                        long durationMs = Math.max(1, (endNano - startNano) / 1_000_000L);

                        // Parse attributes and AI metadata
                        Map<String, Object> attributesMap = new HashMap<>();
                        String modelName = null;
                        int promptTokens = 0;
                        int completionTokens = 0;

                        if (sNode.has("attributes")) {
                            for (JsonNode attr : sNode.get("attributes")) {
                                String key = attr.path("key").asText();
                                Object val = extractValueAsObject(attr.path("value"));
                                attributesMap.put(key, val);

                                if ("gen_ai.request.model".equals(key) || "ai.model".equals(key)) {
                                    modelName = String.valueOf(val);
                                } else if ("gen_ai.usage.prompt_tokens".equals(key)) {
                                    promptTokens = parseInteger(val);
                                } else if ("gen_ai.usage.completion_tokens".equals(key)) {
                                    completionTokens = parseInteger(val);
                                }
                            }
                        }

                        // Determine status
                        String status = "OK";
                        String statusMsg = null;
                        if (sNode.has("status")) {
                            int statusCode = sNode.path("status").path("code").asInt(1);
                            if (statusCode == 2) { // 2 = STATUS_CODE_ERROR in OTLP
                                status = "ERROR";
                                statusMsg = sNode.path("status").path("message").asText(null);
                            }
                        }

                        // Determine span kind
                        String spanKind = "INTERNAL";
                        if (modelName != null || name.toLowerCase().contains("model") || name.toLowerCase().contains("chat")) {
                            spanKind = "MODEL";
                        } else if (name.toLowerCase().contains("tool") || attributesMap.containsKey("tool.name")) {
                            spanKind = "TOOL";
                        } else if (parentSpanId == null) {
                            spanKind = "AGENT";
                        }

                        // Persist span
                        Span span = Span.builder()
                                .id(UUID.randomUUID().toString())
                                .traceId(traceId)
                                .spanId(spanId)
                                .parentSpanId(parentSpanId)
                                .project(project)
                                .operationName(name)
                                .spanKind(spanKind)
                                .serviceName(serviceName)
                                .status(status)
                                .statusMessage(statusMsg)
                                .startTime(startInstant)
                                .endTime(endInstant)
                                .durationMs(durationMs)
                                .attributes(objectMapper.writeValueAsString(attributesMap))
                                .modelName(modelName)
                                .promptTokens(promptTokens)
                                .completionTokens(completionTokens)
                                .totalTokens(promptTokens + completionTokens)
                                .createdAt(Instant.now())
                                .build();

                        spanRepository.save(span);
                        ingestedSpansCount++;
                        traceIdsAffected.add(traceId);
                    }
                }
            }

            // Sync aggregate Trace records for all affected traceIds
            for (String traceId : traceIdsAffected) {
                updateTraceAggregate(traceId);
                // Publish async TraceReceivedEvent to Kafka / Event bus
                eventPublisher.publishTraceReceived(traceId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("ingestedSpans", ingestedSpansCount);
            response.put("tracesProcessed", traceIdsAffected.size());
            return response;

        } catch (Exception e) {
            log.error("Failed to ingest OTLP traces: {}", e.getMessage(), e);
            throw new IllegalArgumentException("OTLP ingestion error: " + e.getMessage(), e);
        }
    }

    private void updateTraceAggregate(String traceId) {
        List<Span> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        if (spans.isEmpty()) return;

        Span rootOrFirst = spans.stream()
                .filter(s -> s.getParentSpanId() == null)
                .findFirst()
                .orElse(spans.get(0));

        Instant earliest = spans.stream().map(Span::getStartTime).min(Comparator.naturalOrder()).orElse(Instant.now());
        Instant latest = spans.stream().map(Span::getEndTime).max(Comparator.naturalOrder()).orElse(Instant.now());
        long totalDurationMs = Math.max(1, latest.toEpochMilli() - earliest.toEpochMilli());

        boolean hasError = spans.stream().anyMatch(s -> "ERROR".equalsIgnoreCase(s.getStatus()));
        int totalPromptTokens = spans.stream().mapToInt(s -> s.getPromptTokens() != null ? s.getPromptTokens() : 0).sum();
        int totalCompletionTokens = spans.stream().mapToInt(s -> s.getCompletionTokens() != null ? s.getCompletionTokens() : 0).sum();
        String modelName = spans.stream().map(Span::getModelName).filter(Objects::nonNull).findFirst().orElse("gpt-4o");

        double cost = (totalPromptTokens / 1000.0 * defaultPromptRate) + (totalCompletionTokens / 1000.0 * defaultCompletionRate);

        Trace trace = traceRepository.findByTraceId(traceId).orElse(null);
        if (trace == null) {
            trace = Trace.builder()
                    .id(UUID.randomUUID().toString())
                    .traceId(traceId)
                    .project(rootOrFirst.getProject())
                    .rootOperationName(rootOrFirst.getOperationName())
                    .serviceName(rootOrFirst.getServiceName())
                    .status(hasError ? "ERROR" : "OK")
                    .startTime(earliest)
                    .endTime(latest)
                    .durationMs(totalDurationMs)
                    .totalSpans(spans.size())
                    .modelName(modelName)
                    .promptTokens(totalPromptTokens)
                    .completionTokens(totalCompletionTokens)
                    .totalTokens(totalPromptTokens + totalCompletionTokens)
                    .estimatedCost(BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP))
                    .createdAt(Instant.now())
                    .build();
        } else {
            trace.setStatus(hasError ? "ERROR" : "OK");
            trace.setStartTime(earliest);
            trace.setEndTime(latest);
            trace.setDurationMs(totalDurationMs);
            trace.setTotalSpans(spans.size());
            trace.setModelName(modelName);
            trace.setPromptTokens(totalPromptTokens);
            trace.setCompletionTokens(totalCompletionTokens);
            trace.setTotalTokens(totalPromptTokens + totalCompletionTokens);
            trace.setEstimatedCost(BigDecimal.valueOf(cost).setScale(6, RoundingMode.HALF_UP));
        }

        traceRepository.save(trace);
    }

    private String extractValueAsString(JsonNode valueNode) {
        if (valueNode == null) return "";
        if (valueNode.has("stringValue")) return valueNode.get("stringValue").asText();
        if (valueNode.has("intValue")) return valueNode.get("intValue").asText();
        return valueNode.asText();
    }

    private Object extractValueAsObject(JsonNode valueNode) {
        if (valueNode == null) return null;
        if (valueNode.has("stringValue")) return valueNode.get("stringValue").asText();
        if (valueNode.has("intValue")) return valueNode.get("intValue").asLong();
        if (valueNode.has("doubleValue")) return valueNode.get("doubleValue").asDouble();
        if (valueNode.has("boolValue")) return valueNode.get("boolValue").asBoolean();
        return valueNode.asText();
    }

    private long parseNano(String text) {
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            return System.currentTimeMillis() * 1_000_000L;
        }
    }

    private int parseInteger(Object val) {
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (Exception e) {
            return 0;
        }
    }
}
