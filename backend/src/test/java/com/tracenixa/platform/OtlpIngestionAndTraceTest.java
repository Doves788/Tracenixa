package com.tracenixa.platform;

import com.tracenixa.platform.ingestion.service.OtlpIngestionService;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.trace.dto.TraceDtos.TraceDetailResponse;
import com.tracenixa.platform.trace.model.Span;
import com.tracenixa.platform.trace.model.Trace;
import com.tracenixa.platform.trace.repository.SpanRepository;
import com.tracenixa.platform.trace.repository.TraceRepository;
import com.tracenixa.platform.trace.service.TraceQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class OtlpIngestionAndTraceTest {

    @Autowired
    private OtlpIngestionService ingestionService;

    @Autowired
    private TraceQueryService traceQueryService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private SpanRepository spanRepository;

    @Test
    @DisplayName("Ingesting OTLP standard JSON trace persists normalized spans and builds hierarchical timeline")
    void testOtlpIngestion() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        String traceId = "testotlp" + System.currentTimeMillis();

        String otlpJson = """
        {
          "resourceSpans": [
            {
              "resource": {
                "attributes": [
                  { "key": "service.name", "value": { "stringValue": "external-customer-agent" } },
                  { "key": "project.id", "value": { "stringValue": "%s" } }
                ]
              },
              "scopeSpans": [
                {
                  "scope": { "name": "opentelemetry" },
                  "spans": [
                    {
                      "traceId": "%s",
                      "spanId": "root001",
                      "name": "agent.resolve_ticket",
                      "kind": 1,
                      "startTimeUnixNano": "1700000000000000000",
                      "endTimeUnixNano": "1700000000500000000",
                      "attributes": [
                        { "key": "gen_ai.request.model", "value": { "stringValue": "gpt-4o" } },
                        { "key": "gen_ai.usage.prompt_tokens", "value": { "intValue": 210 } },
                        { "key": "gen_ai.usage.completion_tokens", "value": { "intValue": 80 } }
                      ],
                      "status": { "code": 1 }
                    },
                    {
                      "traceId": "%s",
                      "spanId": "child002",
                      "parentSpanId": "root001",
                      "name": "tool.check_inventory",
                      "kind": 3,
                      "startTimeUnixNano": "1700000000100000000",
                      "endTimeUnixNano": "1700000000250000000",
                      "attributes": [
                        { "key": "tool.name", "value": { "stringValue": "checkInventory" } }
                      ],
                      "status": { "code": 1 }
                    }
                  ]
                }
              ]
            }
          ]
        }
        """.formatted(project.getId(), traceId, traceId);

        var ingestRes = ingestionService.ingestOtlpTraces(otlpJson, project.getId());
        assertThat(ingestRes.get("status")).isEqualTo("SUCCESS");
        assertThat(ingestRes.get("ingestedSpans")).isEqualTo(2);

        Trace trace = traceRepository.findByTraceId(traceId).orElseThrow();
        assertThat(trace.getServiceName()).isEqualTo("external-customer-agent");
        assertThat(trace.getTotalTokens()).isEqualTo(290);
        assertThat(trace.getRootOperationName()).isEqualTo("agent.resolve_ticket");

        List<Span> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(traceId);
        assertThat(spans).hasSize(2);

        // Verify TraceQueryService builds tree and Gantt percentages
        TraceDetailResponse detail = traceQueryService.getTraceDetail(project.getId(), traceId);
        assertThat(detail.getSpanTree()).hasSize(1);
        assertThat(detail.getSpanTree().get(0).getChildren()).hasSize(1);
        assertThat(detail.getChronologicalSpans().get(0).getDurationMs()).isGreaterThan(0);
    }
}
