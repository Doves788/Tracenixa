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

    @Test
    @DisplayName("BFS level-order traversal logs branch divergence and omitted nodes")
    void testBfsDualGraphTraversal() {
        List<Span> original = List.of(
                createSpan("s1", "agent.root", "INTERNAL", "{}", "{}", "OK", 100),
                createSpan("s2", "tool.checkInventory", "TOOL", "{}", "{}", "OK", 50),
                createSpan("s3", "tool.refundOrder", "TOOL", "{}", "{}", "OK", 80)
        );

        List<Span> replayed = List.of(
                createSpan("s1-rep", "agent.root", "INTERNAL", "{}", "{}", "OK", 100),
                createSpan("s2-rep", "tool.escalateToHuman", "TOOL", "{}", "{}", "OK", 60)
        );

        List<String> logs = detector.traverseAndExplainBfsDiff(original, replayed);

        assertThat(logs).hasSize(3);
        assertThat(logs.get(0)).contains("BFS Level 1: Node [agent.root] matched");
        assertThat(logs.get(1)).contains("BFS Level 2: Divergence branch!");
        assertThat(logs.get(2)).contains("BFS Level 3: Omitted node in G_rep: [tool.refundOrder]");
    }

    @Test
    @DisplayName("Needleman-Wunsch dynamic programming realigns execution path when LLM injects intermediate self-correction")
    void testNeedlemanWunschPathRealignmentUnderSelfCorrection() {
        // Baseline: 5 spans
        List<Span> original = List.of(
                createSpan("s1", "agent.start", "INTERNAL", "{}", "{}", "OK", 50),
                createSpan("s2", "model.generate", "LLM", "{}", "{}", "OK", 400),
                createSpan("s3", "tool.fetch_balance", "TOOL", "{\"account\":\"A100\"}", "{\"bal\":5000}", "OK", 60),
                createSpan("s4", "tool.execute_transfer", "TOOL", "{\"to\":\"B200\",\"amt\":100}", "{\"status\":\"SUCCESS\"}", "OK", 120),
                createSpan("s5", "agent.finish", "INTERNAL", "{}", "{}", "OK", 20)
        );

        // Replayed trace: LLM injects intermediate "model.self_correction" at step 3,
        // then executes the exact expected tool sequence!
        List<Span> replayed = List.of(
                createSpan("s1-rep", "agent.start", "INTERNAL", "{}", "{}", "OK", 52),
                createSpan("s2-rep", "model.generate", "LLM", "{}", "{}", "OK", 410),
                createSpan("s2b-rep", "model.self_correction", "LLM", "{\"thought\":\"rechecking account\"}", "{}", "OK", 150),
                createSpan("s3-rep", "tool.fetch_balance", "TOOL", "{\"account\":\"A100\"}", "{\"bal\":5000}", "OK", 65),
                createSpan("s4-rep", "tool.execute_transfer", "TOOL", "{\"to\":\"B200\",\"amt\":100}", "{\"status\":\"SUCCESS\"}", "OK", 118),
                createSpan("s5-rep", "agent.finish", "INTERNAL", "{}", "{}", "OK", 22)
        );

        // 1. Strict BFS would report false-positive failure on all steps after step 2:
        List<String> bfsLogs = detector.traverseAndExplainBfsDiff(original, replayed);
        assertThat(bfsLogs.get(2)).contains("Divergence branch"); // tool.fetch_balance vs model.self_correction
        assertThat(bfsLogs.get(3)).contains("Divergence branch"); // tool.execute_transfer vs tool.fetch_balance
        assertThat(bfsLogs.get(4)).contains("Divergence branch"); // agent.finish vs tool.execute_transfer

        // 2. Needleman-Wunsch DP algorithm cleanly isolates the inserted self-correction step
        // and realigns all downstream tool calls with 100% parity!
        ExecutionDivergenceDetector.SequenceAlignmentResult alignment =
                detector.alignSequencesNeedlemanWunsch(original, replayed);

        assertThat(alignment.isPathRealigned()).isTrue();
        assertThat(alignment.getMatchedCount()).isEqualTo(5);
        assertThat(alignment.getInsertedCount()).isEqualTo(1);
        assertThat(alignment.getDeletedCount()).isEqualTo(0);
        assertThat(alignment.getMutatedCount()).isEqualTo(0);
        assertThat(alignment.getAlignedSteps()).hasSize(6);

        // Verify that step 3 is identified as an isolated INSERTION
        ExecutionDivergenceDetector.AlignedSpanStep step3 = alignment.getAlignedSteps().get(2);
        assertThat(step3.getAlignmentType()).isEqualTo(ExecutionDivergenceDetector.AlignmentType.INSERTION);
        assertThat(step3.getReplayedSpan().getOperationName()).isEqualTo("model.self_correction");

        // Verify that step 4 is realigned as MATCH for tool.fetch_balance
        ExecutionDivergenceDetector.AlignedSpanStep step4 = alignment.getAlignedSteps().get(3);
        assertThat(step4.getAlignmentType()).isEqualTo(ExecutionDivergenceDetector.AlignmentType.MATCH);
        assertThat(step4.getOriginalSpan().getOperationName()).isEqualTo("tool.fetch_balance");
        assertThat(step4.getReplayedSpan().getOperationName()).isEqualTo("tool.fetch_balance");

        // Verify that step 5 is realigned as MATCH for tool.execute_transfer
        ExecutionDivergenceDetector.AlignedSpanStep step5 = alignment.getAlignedSteps().get(4);
        assertThat(step5.getAlignmentType()).isEqualTo(ExecutionDivergenceDetector.AlignmentType.MATCH);
        assertThat(step5.getOriginalSpan().getOperationName()).isEqualTo("tool.execute_transfer");
        assertThat(step5.getReplayedSpan().getOperationName()).isEqualTo("tool.execute_transfer");
    }
}

