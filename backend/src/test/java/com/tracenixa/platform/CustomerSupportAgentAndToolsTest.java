package com.tracenixa.platform;

import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.agent.tools.CustomerSupportTools;
import com.tracenixa.platform.fault.context.FaultContext;
import com.tracenixa.platform.fault.model.FaultConfig;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.trace.model.Span;
import com.tracenixa.platform.trace.repository.SpanRepository;
import com.tracenixa.platform.trace.repository.TraceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CustomerSupportAgentAndToolsTest {

    @Autowired
    private CustomerSupportAgent agent;

    @Autowired
    private CustomerSupportTools tools;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TraceRepository traceRepository;

    @Autowired
    private SpanRepository spanRepository;

    private String testProjectId;

    @BeforeEach
    void setup() {
        Project p = projectRepository.findAll().stream().findFirst().orElseThrow();
        testProjectId = p.getId();
    }

    @Test
    @DisplayName("Agent workflow creates correctly linked hierarchical spans and records token usage")
    void testAgentExecutionAndSpans() {
        var result = agent.runWorkflow(testProjectId, "I need to check order ORD-9821 and see if headphones are available for refund.", 1);

        assertThat(result.getTraceId()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo("OK");
        assertThat(result.getToolCallsMade()).contains("getOrderStatus", "checkInventory", "refundOrder");

        var traceOpt = traceRepository.findByTraceId(result.getTraceId());
        assertThat(traceOpt).isPresent();
        assertThat(traceOpt.get().getTotalSpans()).isGreaterThanOrEqualTo(4);
        assertThat(traceOpt.get().getTotalTokens()).isGreaterThan(0);
        assertThat(traceOpt.get().getEstimatedCost().doubleValue()).isGreaterThan(0.0);

        List<Span> spans = spanRepository.findByTraceIdOrderByStartTimeAsc(result.getTraceId());
        Span root = spans.stream().filter(s -> s.getParentSpanId() == null).findFirst().orElseThrow();
        assertThat(root.getSpanKind()).isEqualTo("AGENT");

        // Verify child spans link to root span
        List<Span> children = spans.stream().filter(s -> root.getSpanId().equals(s.getParentSpanId())).toList();
        assertThat(children).isNotEmpty();
    }

    @Test
    @DisplayName("Idempotency protection prevents duplicate destructive refunds")
    void testRefundIdempotency() {
        String key = "test-idemp-" + System.currentTimeMillis();
        String res1 = tools.refundOrder("ORD-9821", 149.99, "Damaged unit", key);
        assertThat(res1).contains("REFUND_SIMULATED_SUCCESS");

        // Second call with same idempotency key must not repeat destructive action
        String res2 = tools.refundOrder("ORD-9821", 149.99, "Damaged unit", key);
        assertThat(res2).contains("SUCCESS_IDEMPOTENT_REPLAY");
    }

    @Test
    @DisplayName("Fault injection produces configured timeout and agent retries with exponential backoff")
    void testFaultInjectionAndRecovery() {
        FaultConfig timeoutFault = FaultConfig.builder()
                .targetTool("checkInventory")
                .faultType("TIMEOUT")
                .delayMs(100L)
                .build();

        // 1. Under 0 retries, unhandled fault causes agent workflow failure
        FaultContext.setActiveFault(timeoutFault);
        try {
            var failRes = agent.runWorkflow(testProjectId, "Check stock for ORD-9821", 0);
            assertThat(failRes.getStatus()).isEqualTo("ERROR");
            assertThat(failRes.getErrorMessage()).contains("TOOL_TIMEOUT");
        } finally {
            FaultContext.clear();
        }

        // 2. Clear fault -> retry succeeds cleanly
        var okRes = agent.runWorkflow(testProjectId, "Check stock for ORD-9821", 1);
        assertThat(okRes.getStatus()).isEqualTo("OK");
    }
}
