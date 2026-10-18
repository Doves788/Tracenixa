package com.tracenixa.platform.ingestion.controller;

import com.tracenixa.platform.ingestion.service.OtlpIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/traces")
@RequiredArgsConstructor
@Tag(name = "OpenTelemetry Ingestion", description = "OTLP/HTTP standard trace receiver endpoint")
public class OtlpIngestionController {

    private final OtlpIngestionService ingestionService;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "application/x-protobuf", "*/*"})
    @Operation(summary = "Ingest OpenTelemetry OTLP standard trace batch (JSON formatted)")
    public ResponseEntity<Map<String, Object>> ingestTraces(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Project-Id", required = false) String projectIdHeader) {

        Map<String, Object> result = ingestionService.ingestOtlpTraces(rawPayload, projectIdHeader);
        return ResponseEntity.ok(result);
    }
}
