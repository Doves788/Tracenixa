package com.tracenixa.platform;

import com.tracenixa.platform.fault.model.FaultConfig;
import com.tracenixa.platform.replay.divergence.ExecutionDivergenceDetector;
import com.tracenixa.platform.replay.divergence.ExecutionDivergenceDetector.DivergenceResult;
import com.tracenixa.platform.trace.model.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionDivergenceDetectorTest {

    private ExecutionDivergenceDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ExecutionDivergenceDetector();
    }

    private Span createSpan(String spanId, String opName, String kind, String input, String output, String status, long durationMs) {
        return Span.builder()
                .id(spanId)
                .spanId(spanId)
                .operationName(opName)
                .spanKind(kind)
                .inputPayload(input)
                .outputPayload(output)
                .status(status)
                .durationMs(durationMs)
                .startTime(Instant.now())
                .endTime(Instant.now().plusMillis(durationMs))
                .build();
    }

    @Test
    @DisplayName("Identical execution produces 100% path parity, zero divergence, and NONE severity")
    void testIdenticalExecutionParity() {
        List<Span> original = List.of(
                createSpan("s1", "agent.customer_support", "INTERNAL", "Refund ORD-9821", "Processed", "OK", 300),
                createSpan("s2", "tool.getOrderStatus", "TOOL", "{\"orderId\":\"ORD-9821\"}", "{\"status\":\"DELIVERED\"}", "OK", 40),
                createSpan("s3", "tool.checkInventory", "TOOL", "{\"sku\":\"HEADPHONES\"}", "{\"inStock\":true}", "OK", 50),
                createSpan("s4", "tool.refundOrder", "TOOL", "{\"amount\":89.99}", "{\"refundId\":\"REF-1\"}", "OK", 80)
        );

        List<Span> replayed = List.of(
                createSpan("s1-rep", "agent.customer_support", "INTERNAL", "Refund ORD-9821", "Processed", "OK", 310),
                createSpan("s2-rep", "tool.getOrderStatus", "TOOL", "{\"orderId\":\"ORD-9821\"}", "{\"status\":\"DELIVERED\"}", "OK", 42),
                createSpan("s3-rep", "tool.checkInventory", "TOOL", "{\"sku\":\"HEADPHONES\"}", "{\"inStock\":true}", "OK", 49),
                createSpan("s4-rep", "tool.refundOrder", "TOOL", "{\"amount\":89.99}", "{\"refundId\":\"REF-1\"}", "OK", 82)
        );

        DivergenceResult res = detector.analyze("RECORDED", original, replayed, null);

        assertThat(res.getAnalysis().isDiverged()).isFalse();
        assertThat(res.getAnalysis().getSimilarityScore()).isEqualTo(1.0);
        assertThat(res.getAnalysis().getSeverity()).isEqualTo("NONE");
        assertThat(res.getAnalysis().getRootCauseCategory()).isEqualTo("NONE");
        assertThat(res.getAnalysis().getMatchedSteps()).isEqualTo(4);
        assertThat(res.getAnalysis().getDivergedSteps()).isEqualTo(0);
    }

    @Test
    @DisplayName("Controlled fault injection correctly identifies INJECTED_FAULT with LOW severity")
    void testControlledFaultInjection() {
        List<Span> original = List.of(
                createSpan("s1", "tool.checkInventory", "TOOL", "{\"sku\":\"HEADPHONES\"}", "{\"inStock\":true}", "OK", 50)
        );

        List<Span> replayed = List.of(
                createSpan("s1-rep", "tool.checkInventory", "TOOL", "{\"sku\":\"HEADPHONES\"}", "{\"error\":\"Timeout\"}", "ERROR", 1500)
        );

        FaultConfig fault = FaultConfig.builder()
                .targetTool("checkInventory")
                .faultType("TIMEOUT")
                .delayMs(1500L)
                .build();

        DivergenceResult res = detector.analyze("FAULT_INJECTION", original, replayed, fault);

        assertThat(res.getAnalysis().getRootCauseCategory()).isEqualTo("INJECTED_FAULT");
        assertThat(res.getAnalysis().getSeverity()).isEqualTo("LOW");
        assertThat(res.getStepComparisons().get(0).getStatus()).isEqualTo("INJECTED_FAULT");
    }

    @Test
    @DisplayName("Tool sequence branching detects CRITICAL TOOL_SEQUENCE_MISMATCH")
    void testToolSequenceMismatch() {
        List<Span> original = List.of(
                createSpan("s1", "tool.getOrderStatus", "TOOL", "{\"orderId\":\"ORD-9821\"}", "{}", "OK", 40),
                createSpan("s2", "tool.refundOrder", "TOOL", "{\"amount\":89.99}", "{}", "OK", 80)
        );

        List<Span> replayed = List.of(
                createSpan("s1-rep", "tool.getOrderStatus", "TOOL", "{\"orderId\":\"ORD-9821\"}", "{}", "OK", 40),
                createSpan("s2-rep", "tool.escalateToHuman", "TOOL", "{\"reason\":\"UNSURE\"}", "{}", "OK", 100)
        );

        DivergenceResult res = detector.analyze("LIVE", original, replayed, null);

        assertThat(res.getAnalysis().isDiverged()).isTrue();
        assertThat(res.getAnalysis().getSeverity()).isEqualTo("CRITICAL");
        assertThat(res.getAnalysis().getRootCauseCategory()).isEqualTo("TOOL_SEQUENCE_MISMATCH");
        assertThat(res.getAnalysis().getFirstDivergenceStep()).isEqualTo(2);
        assertThat(res.getAnalysis().getSimilarityScore()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Tool parameter drift detects MEDIUM PARAMETER_DRIFT")
    void testParameterDriftDetection() {
        List<Span> original = List.of(
                createSpan("s1", "tool.refundOrder", "TOOL", "{\"amount\":89.99,\"orderId\":\"ORD-9821\"}", "{\"ok\":true}", "OK", 80)
        );

        List<Span> replayed = List.of(
                createSpan("s1-rep", "tool.refundOrder", "TOOL", "{\"amount\":50.00,\"orderId\":\"ORD-9821\"}", "{\"ok\":true}", "OK", 80)
        );

        DivergenceResult res = detector.analyze("LIVE", original, replayed, null);

        assertThat(res.getAnalysis().isDiverged()).isTrue();
        assertThat(res.getAnalysis().getSeverity()).isEqualTo("MEDIUM");
        assertThat(res.getAnalysis().getRootCauseCategory()).isEqualTo("PARAMETER_DRIFT");
        assertThat(res.getAnalysis().getFirstDivergenceStep()).isEqualTo(1);
    }
}

