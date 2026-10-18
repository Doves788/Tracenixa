package com.tracenixa.platform.analytics.controller;

import com.tracenixa.platform.analytics.dto.AnalyticsDtos.*;
import com.tracenixa.platform.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Cost Monitoring", description = "Performance metrics, p50/p95 latency, model costs, and tool failure rates")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get high-level project KPIs: total traces, success rate, p50/p95 latency, total tokens, and estimated cost")
    public ResponseEntity<OverviewStatsDto> getOverview(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getOverviewStats(projectId));
    }

    @GetMapping("/models")
    @Operation(summary = "Get token consumption and estimated cost breakdown per AI model")
    public ResponseEntity<List<ModelCostBreakdownDto>> getModelCosts(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getModelCosts(projectId));
    }

    @GetMapping("/tools")
    @Operation(summary = "Get execution count, average latency, and failure rate breakdown for each agent tool")
    public ResponseEntity<List<ToolPerformanceDto>> getToolPerformance(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getToolPerformance(projectId));
    }

    @GetMapping("/latency")
    @Operation(summary = "Get latency distribution histogram buckets (0-200ms, 200-500ms, etc.)")
    public ResponseEntity<List<LatencyBucketDto>> getLatencyHistogram(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getLatencyHistogram(projectId));
    }

    @GetMapping("/recent-failures")
    @Operation(summary = "Get list of recent failed agent executions for rapid debugging")
    public ResponseEntity<List<RecentFailureDto>> getRecentFailures(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getRecentFailures(projectId));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get 7-day daily time-series trend of executions, errors, token volume, and costs")
    public ResponseEntity<List<DailyTrendDto>> getDailyTimeline(@PathVariable String projectId) {
        return ResponseEntity.ok(analyticsService.getDailyTrends(projectId));
    }
}
