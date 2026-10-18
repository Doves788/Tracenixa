package com.tracenixa.platform.trace.controller;

import com.tracenixa.platform.trace.dto.TraceDtos.*;
import com.tracenixa.platform.trace.service.TraceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/traces")
@RequiredArgsConstructor
@Tag(name = "Traces & Spans", description = "Trace search, collapsible span tree, and execution timeline")
public class TraceController {

    private final TraceQueryService traceQueryService;

    @GetMapping
    @Operation(summary = "Query and paginate traces with filters for status, service, and search query")
    public ResponseEntity<Page<TraceSummaryResponse>> getTraces(
            @PathVariable String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(traceQueryService.getTraces(projectId, status, serviceName, search, page, size));
    }

    @GetMapping("/{traceId}")
    @Operation(summary = "Get detailed trace with hierarchical collapsible span tree and Gantt execution timeline")
    public ResponseEntity<TraceDetailResponse> getTraceDetail(
            @PathVariable String projectId,
            @PathVariable String traceId) {

        return ResponseEntity.ok(traceQueryService.getTraceDetail(projectId, traceId));
    }
}
