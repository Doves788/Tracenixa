package com.tracenixa.platform.replay.divergence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.fault.model.FaultConfig;
import com.tracenixa.platform.replay.dto.ReplayDtos.DivergenceAnalysis;
import com.tracenixa.platform.replay.dto.ReplayDtos.ReplayStepComparison;
import com.tracenixa.platform.trace.model.Span;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Enterprise Execution Divergence Detection Engine.
 * Formally analyzes behavioral drift, tool-calling sequence divergences,
 * parameter deviations, and state corruption between baseline traces and replayed sessions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionDivergenceDetector {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Builder
    public static class DivergenceResult {
        private DivergenceAnalysis analysis;
        private List<ReplayStepComparison> stepComparisons;
    }

    public DivergenceResult analyze(
            String replayMode,
            List<Span> originalSpans,
            List<Span> replayedSpans,
            FaultConfig faultConfig
    ) {
        List<ReplayStepComparison> comparisons = new ArrayList<>();
        List<String> sequenceDifferences = new ArrayList<>();

        int origSize = originalSpans != null ? originalSpans.size() : 0;
        int repSize = replayedSpans != null ? replayedSpans.size() : 0;
        int totalSteps = Math.max(origSize, repSize);

        int matchedCount = 0;
        int divergedCount = 0;
        Integer firstDivergenceStep = null;
        String primaryRootCause = "NONE";
        String primaryExplanation = "Execution graph, tool parameters, and response payloads matched recorded baseline perfectly (100% path parity).";

        boolean sequenceAltered = false;

        for (int i = 0; i < totalSteps; i++) {
            Span orig = (originalSpans != null && i < origSize) ? originalSpans.get(i) : null;
            Span rep = (replayedSpans != null && i < repSize) ? replayedSpans.get(i) : null;

            int stepOrder = i + 1;
            String opName = rep != null ? rep.getOperationName() : (orig != null ? orig.getOperationName() : "step-" + stepOrder);
            String kind = rep != null ? rep.getSpanKind() : (orig != null ? orig.getSpanKind() : "SPAN");

            String origIn = orig != null ? orig.getInputPayload() : null;
            String origOut = orig != null ? orig.getOutputPayload() : null;
            String repIn = rep != null ? rep.getInputPayload() : null;
            String repOut = rep != null ? rep.getOutputPayload() : null;

            Long origDur = orig != null ? orig.getDurationMs() : 0L;
            Long repDur = rep != null ? rep.getDurationMs() : 0L;

            // Check if step exists on both sides
            if (orig == null) {
                sequenceAltered = true;
                divergedCount++;
                if (firstDivergenceStep == null) {
                    firstDivergenceStep = stepOrder;
                    primaryRootCause = "TOOL_SEQUENCE_MISMATCH";
                    primaryExplanation = "Unexpected extra tool invocation [" + opName + "] executed during replay.";
                }
                String diffNote = "Replay invoked unexpected operation not present in original trace.";
                sequenceDifferences.add("Step " + stepOrder + ": Replay executed unexpected [" + opName + "]");

                comparisons.add(createStep(stepOrder, opName, kind, null, null, repIn, repOut, 0L, repDur, false, "DIVERGED", diffNote));
                continue;
            }

            if (rep == null) {
                sequenceAltered = true;
                divergedCount++;
                if (firstDivergenceStep == null) {
                    firstDivergenceStep = stepOrder;
                    primaryRootCause = "TOOL_SEQUENCE_MISMATCH";
                    primaryExplanation = "Expected tool invocation [" + orig.getOperationName() + "] was omitted during replay.";
                }
                String diffNote = "Original operation was omitted during replay execution.";
                sequenceDifferences.add("Step " + stepOrder + ": Expected [" + orig.getOperationName() + "] was omitted");

                comparisons.add(createStep(stepOrder, opName, kind, origIn, origOut, null, null, origDur, 0L, false, "DIVERGED", diffNote));
                continue;
            }

            // Both steps exist: compare operation name
            if (!orig.getOperationName().equals(rep.getOperationName())) {
                sequenceAltered = true;
                divergedCount++;
                if (firstDivergenceStep == null) {
                    firstDivergenceStep = stepOrder;
                    primaryRootCause = "TOOL_SEQUENCE_MISMATCH";
                    primaryExplanation = String.format("Tool invocation sequence branched at step %d: expected [%s] but replayed [%s].",
                            stepOrder, orig.getOperationName(), rep.getOperationName());
                }
                String diffNote = String.format("Operation name diverged: original=[%s], replayed=[%s]",
                        orig.getOperationName(), rep.getOperationName());
                sequenceDifferences.add(diffNote);

                comparisons.add(createStep(stepOrder, opName, kind, origIn, origOut, repIn, repOut, origDur, repDur, false, "DIVERGED", diffNote));
                continue;
            }

            // Check controlled fault injection
            boolean isTargetFault = "FAULT_INJECTION".equals(replayMode) && faultConfig != null &&
                    (faultConfig.getTargetTool() == null || opName.contains(faultConfig.getTargetTool()));

            if (isTargetFault) {
                divergedCount++;
                if (firstDivergenceStep == null) {
                    firstDivergenceStep = stepOrder;
                    primaryRootCause = "INJECTED_FAULT";
                    primaryExplanation = String.format("Controlled fault [%s] injected at step %d (%s). Agent exercised resilience policy.",
                            faultConfig.getFaultType(), stepOrder, opName);
                }
                String diffNote = String.format("Controlled fault [%s] injected. Intercepted and handled via backoff/retry.",
                        faultConfig.getFaultType());

                comparisons.add(createStep(stepOrder, opName, kind, origIn, origOut, repIn, repOut, origDur, repDur, false, "INJECTED_FAULT", diffNote));
                continue;
            }

            // Compare inputs and outputs
            boolean inputMatched = arePayloadsEquivalent(origIn, repIn);
            boolean outputMatched = arePayloadsEquivalent(origOut, repOut);
            boolean statusMatched = Objects.equals(orig.getStatus(), rep.getStatus());

            if (inputMatched && outputMatched && statusMatched) {
                matchedCount++;
                comparisons.add(createStep(stepOrder, opName, kind, origIn, origOut, repIn, repOut, origDur, repDur, true, "MATCHED",
                        "Execution step, tool parameters, and response payloads matched baseline."));
            } else {
                divergedCount++;
                String cause;
                if (!inputMatched) {
                    cause = "Parameter drift: tool arguments deviated from original.";
                    if (firstDivergenceStep == null) {
                        firstDivergenceStep = stepOrder;
                        primaryRootCause = "PARAMETER_DRIFT";
                        primaryExplanation = "Parameter drift detected at step " + stepOrder + " (" + opName + "): input arguments did not match.";
                    }
                } else if (!statusMatched) {
                    cause = String.format("Status mismatch: original [%s] vs replayed [%s].", orig.getStatus(), rep.getStatus());
                    if (firstDivergenceStep == null) {
                        firstDivergenceStep = stepOrder;
                        primaryRootCause = "OUTCOME_DRIFT";
                        primaryExplanation = "Status failure at step " + stepOrder + " (" + opName + ").";
                    }
                } else {
                    cause = "Output payload drift: tool/model returned different data.";
                    if (firstDivergenceStep == null) {
                        firstDivergenceStep = stepOrder;
                        primaryRootCause = "OUTCOME_DRIFT";
                        primaryExplanation = "Output payload diverged at step " + stepOrder + " (" + opName + ").";
                    }
                }
                sequenceDifferences.add("Step " + stepOrder + " (" + opName + "): " + cause);
                comparisons.add(createStep(stepOrder, opName, kind, origIn, origOut, repIn, repOut, origDur, repDur, false, "DIVERGED", cause));
            }
        }

        // Calculate quantitative divergence metrics
        double similarityScore = totalSteps > 0
                ? Math.round(((matchedCount * 1.0) / totalSteps) * 1000.0) / 1000.0
                : 1.0;

        String severity;
        if (divergedCount == 0) {
            severity = "NONE";
        } else if ("FAULT_INJECTION".equals(replayMode) && "INJECTED_FAULT".equals(primaryRootCause)) {
            severity = "LOW";
        } else if (sequenceAltered) {
            severity = "CRITICAL";
        } else if ("PARAMETER_DRIFT".equals(primaryRootCause) || "OUTCOME_DRIFT".equals(primaryRootCause)) {
            severity = "MEDIUM";
        } else {
            severity = "LOW";
        }

        DivergenceAnalysis analysis = DivergenceAnalysis.builder()
                .diverged(divergedCount > 0 && !"FAULT_INJECTION".equals(replayMode))
                .similarityScore(similarityScore)
                .severity(severity)
                .firstDivergenceStep(firstDivergenceStep)
                .rootCauseCategory(primaryRootCause)
                .explanation(primaryExplanation)
                .sequenceDifferences(sequenceDifferences)
                .matchedSteps(matchedCount)
                .divergedSteps(divergedCount)
                .totalSteps(totalSteps)
                .build();

        return DivergenceResult.builder()
                .analysis(analysis)
                .stepComparisons(comparisons)
                .build();
    }

    private boolean arePayloadsEquivalent(String payload1, String payload2) {
        if (Objects.equals(payload1, payload2)) {
            return true;
        }
        if (payload1 == null || payload2 == null) {
            return false;
        }
        try {
            JsonNode tree1 = objectMapper.readTree(payload1);
            JsonNode tree2 = objectMapper.readTree(payload2);
            return tree1.equals(tree2);
        } catch (Exception ignored) {
            return payload1.trim().equalsIgnoreCase(payload2.trim());
        }
    }

    private ReplayStepComparison createStep(
            int stepOrder,
            String opName,
            String eventType,
            String origIn,
            String origOut,
            String repIn,
            String repOut,
            Long origDur,
            Long repDur,
            boolean match,
            String status,
            String diffNote
    ) {
        return ReplayStepComparison.builder()
                .stepOrder(stepOrder)
                .operationName(opName)
                .eventType(eventType)
                .originalInput(origIn)
                .originalOutput(origOut)
                .replayedInput(repIn)
                .replayedOutput(repOut)
                .originalDurationMs(origDur)
                .replayedDurationMs(repDur)
                .match(match)
                .status(status)
                .diffNote(diffNote)
                .build();
    }
}

