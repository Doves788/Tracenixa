package com.tracenixa.platform.analytics.service;

import com.tracenixa.platform.analytics.dto.AnalyticsDtos.*;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;

    @Value("${tracenixa.pricing.default-prompt-rate-per-1k:0.005}")
    private double defaultPromptRate;

    @Value("${tracenixa.pricing.default-completion-rate-per-1k:0.015}")
    private double defaultCompletionRate;

    @Transactional(readOnly = true)
    public OverviewStatsDto getOverviewStats(String projectId) {
        long totalTraces = traceRepository.countByProjectId(projectId);
        long errorCount = traceRepository.countErrorsByProjectId(projectId);
        Double avgDuration = traceRepository.getAverageDurationByProjectId(projectId);
        Long totalTokens = traceRepository.getTotalTokensByProjectId(projectId);
        Double totalCost = traceRepository.getTotalCostByProjectId(projectId);

        double successRate = totalTraces > 0
                ? Math.round(((totalTraces - errorCount) * 100.0 / totalTraces) * 10.0) / 10.0
                : 100.0;

        // Calculate p50 and p95 latency percentiles
        List<Trace> traces = traceRepository.findByProjectIdOrderByStartTimeDesc(projectId, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
        List<Long> durations = traces.stream().map(Trace::getDurationMs).sorted().collect(Collectors.toList());

        long p50 = 0;
        long p95 = 0;
        if (!durations.isEmpty()) {
            int idx50 = (int) Math.floor(durations.size() * 0.50);
            int idx95 = (int) Math.min(durations.size() - 1, Math.floor(durations.size() * 0.95));
            p50 = durations.get(idx50);
            p95 = durations.get(idx95);
        }

        long promptToks = (totalTokens != null ? totalTokens : 0L) * 3 / 4;
        long compToks = (totalTokens != null ? totalTokens : 0L) / 4;

        return OverviewStatsDto.builder()
                .totalTraces(totalTraces)
                .successRate(successRate)
                .errorCount(errorCount)
                .p50LatencyMs(p50)
                .p95LatencyMs(p95)
                .avgLatencyMs(avgDuration != null ? Math.round(avgDuration * 10.0) / 10.0 : 0.0)
                .totalTokens(totalTokens != null ? totalTokens : 0L)
                .promptTokens(promptToks)
                .completionTokens(compToks)
                .estimatedCostUsd(totalCost != null ? BigDecimal.valueOf(totalCost).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModelCostBreakdownDto> getModelCosts(String projectId) {
        List<Object[]> rows = traceRepository.getTokensByModel(projectId);
        List<ModelCostBreakdownDto> list = new ArrayList<>();

        for (Object[] r : rows) {
            String model = r[0] != null ? String.valueOf(r[0]) : "unknown-model";
            long tokens = r[1] != null ? ((Number) r[1]).longValue() : 0L;
            long count = r[2] != null ? ((Number) r[2]).longValue() : 0L;

            double cost = (tokens / 1000.0) * ((defaultPromptRate + defaultCompletionRate) / 2.0);

            list.add(ModelCostBreakdownDto.builder()
                    .modelName(model)
                    .totalTokens(tokens)
                    .executionCount(count)
                    .estimatedCostUsd(BigDecimal.valueOf(cost).setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        if (list.isEmpty()) {
            list.add(ModelCostBreakdownDto.builder()
                    .modelName("gpt-4o")
                    .totalTokens(0L)
                    .executionCount(0L)
                    .estimatedCostUsd(BigDecimal.ZERO)
                    .build());
        }

        return list;
    }

    @Transactional(readOnly = true)
    public List<ToolPerformanceDto> getToolPerformance(String projectId) {
        List<Object[]> rows = spanRepository.getToolCallMetrics(projectId);
        List<ToolPerformanceDto> list = new ArrayList<>();

        for (Object[] r : rows) {
            String tool = String.valueOf(r[0]);
            long calls = ((Number) r[1]).longValue();
            double avgDur = r[2] != null ? ((Number) r[2]).doubleValue() : 0.0;
            long fails = r[3] != null ? ((Number) r[3]).longValue() : 0L;
            double failRate = calls > 0 ? Math.round((fails * 100.0 / calls) * 10.0) / 10.0 : 0.0;

            list.add(ToolPerformanceDto.builder()
                    .toolName(tool)
                    .callCount(calls)
                    .avgDurationMs(Math.round(avgDur * 10.0) / 10.0)
                    .failureCount(fails)
                    .failureRatePct(failRate)
                    .build());
        }

        return list;
    }

    @Transactional(readOnly = true)
    public List<LatencyBucketDto> getLatencyHistogram(String projectId) {
        List<Trace> traces = traceRepository.findByProjectIdOrderByStartTimeDesc(projectId, org.springframework.data.domain.PageRequest.of(0, 500)).getContent();

        int b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0;
        for (Trace t : traces) {
            long d = t.getDurationMs();
            if (d < 200) b1++;
            else if (d < 500) b2++;
            else if (d < 1000) b3++;
            else if (d < 2000) b4++;
            else b5++;
        }

        return List.of(
                new LatencyBucketDto("0-200ms", b1),
                new LatencyBucketDto("200-500ms", b2),
                new LatencyBucketDto("500-1000ms", b3),
                new LatencyBucketDto("1000-2000ms", b4),
                new LatencyBucketDto(">2000ms", b5)
        );
    }

    @Transactional(readOnly = true)
    public List<RecentFailureDto> getRecentFailures(String projectId) {
        List<Trace> errors = traceRepository.findTop10ByProjectIdAndStatusOrderByStartTimeDesc(projectId, "ERROR");
        return errors.stream()
                .map(t -> RecentFailureDto.builder()
                        .traceId(t.getTraceId())
                        .rootOperation(t.getRootOperationName())
                        .serviceName(t.getServiceName())
                        .errorMessage(t.getErrorMessage() != null ? t.getErrorMessage() : "Unknown execution failure")
                        .durationMs(t.getDurationMs())
                        .timestamp(t.getStartTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DailyTrendDto> getDailyTrends(String projectId) {
        Instant sevenDaysAgo = Instant.now().minusSeconds(86400 * 7);
        List<Trace> traces = traceRepository.findByProjectIdAndStartTimeBetweenOrderByStartTimeAsc(projectId, sevenDaysAgo, Instant.now());

        Map<String, List<Trace>> byDate = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now(ZoneOffset.UTC).minusDays(i);
            byDate.put(d.format(fmt), new ArrayList<>());
        }

        for (Trace t : traces) {
            String dateStr = LocalDate.ofInstant(t.getStartTime(), ZoneOffset.UTC).format(fmt);
            if (byDate.containsKey(dateStr)) {
                byDate.get(dateStr).add(t);
            }
        }

        List<DailyTrendDto> trends = new ArrayList<>();
        for (Map.Entry<String, List<Trace>> entry : byDate.entrySet()) {
            List<Trace> dayTraces = entry.getValue();
            int count = dayTraces.size();
            int errors = (int) dayTraces.stream().filter(t -> "ERROR".equals(t.getStatus())).count();
            long tokens = dayTraces.stream().mapToLong(t -> t.getTotalTokens() != null ? t.getTotalTokens() : 0).sum();
            double cost = dayTraces.stream().mapToDouble(t -> t.getEstimatedCost() != null ? t.getEstimatedCost().doubleValue() : 0.0).sum();
            double avgLatency = count > 0 ? dayTraces.stream().mapToLong(Trace::getDurationMs).average().orElse(0.0) : 0.0;

            trends.add(DailyTrendDto.builder()
                    .date(entry.getKey())
                    .executions(count)
                    .errors(errors)
                    .tokens(tokens)
                    .cost(Math.round(cost * 10000.0) / 10000.0)
                    .avgLatencyMs(Math.round(avgLatency * 10.0) / 10.0)
                    .build());
        }

        return trends;
    }
}
