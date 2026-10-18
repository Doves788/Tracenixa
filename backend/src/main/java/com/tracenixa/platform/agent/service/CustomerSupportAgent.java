package com.tracenixa.platform.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.agent.tools.CustomerSupportTools;
import com.tracenixa.platform.trace.service.TraceRecorder;
import com.tracenixa.platform.trace.service.TraceRecorder.TraceContext;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerSupportAgent {

    private final CustomerSupportTools tools;
    private final TraceRecorder traceRecorder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Builder
    public static class AgentExecutionResult {
        private String traceId;
        private String finalResponse;
        private String status;
        private List<String> toolCallsMade;
        private long totalDurationMs;
        private String errorMessage;
    }

    public AgentExecutionResult runWorkflow(String projectId, String userPrompt, int maxRetries) {
        TraceContext ctx = traceRecorder.startTrace(
                projectId,
                "agent.customer_support",
                "customer-support-agent",
                userPrompt
        );

        List<String> toolsInvoked = new ArrayList<>();
        StringBuilder responseBuilder = new StringBuilder();
        String currentStatus = "OK";
        String fatalError = null;

        try {
            // Step 1: Model Planning & Tool Selection Call
            Instant modelStart1 = Instant.now();
            simulateModelLatency(120);
            Instant modelEnd1 = Instant.now();

            String modelPlanPrompt = "User request: \"" + userPrompt + "\". Plan the required tool calls.";
            String extractedOrderId = extractOrderId(userPrompt);

            traceRecorder.recordModelCall(
                    ctx,
                    "model.intent_analysis",
                    "gpt-4o",
                    145,
                    42,
                    modelStart1,
                    modelEnd1,
                    modelPlanPrompt,
                    "Plan: inspect order status for " + extractedOrderId + ", evaluate inventory, process resolution.",
                    "OK",
                    null
            );

            // Step 2: Tool Execution - getOrderStatus with bounded retry logic
            String orderJson = executeToolWithRetry(
                    ctx,
                    "getOrderStatus",
                    "{\"orderId\": \"" + extractedOrderId + "\"}",
                    () -> tools.getOrderStatus(extractedOrderId),
                    maxRetries,
                    toolsInvoked
            );

            JsonNode orderNode = objectMapper.readTree(orderJson);
            String productId = orderNode.has("productId") ? orderNode.get("productId").asText() : "PROD-WIRELESS-HEADPHONES";
            double amount = orderNode.has("totalAmount") ? orderNode.get("totalAmount").asDouble() : 149.99;

            // Step 3: Tool Execution - checkInventory with bounded retry logic
            String inventoryJson = executeToolWithRetry(
                    ctx,
                    "checkInventory",
                    "{\"productId\": \"" + productId + "\"}",
                    () -> tools.checkInventory(productId),
                    maxRetries,
                    toolsInvoked
            );

            // Step 4: Tool Execution - refundOrder if requested or damaged
            boolean requestRefund = userPrompt.toLowerCase().contains("refund") || userPrompt.toLowerCase().contains("damage");
            String refundJson = null;
            if (requestRefund) {
                String idempotencyKey = "idemp-" + extractedOrderId + "-refund";
                refundJson = executeToolWithRetry(
                        ctx,
                        "refundOrder",
                        "{\"orderId\": \"" + extractedOrderId + "\", \"amount\": " + amount + ", \"idempotencyKey\": \"" + idempotencyKey + "\"}",
                        () -> tools.refundOrder(extractedOrderId, amount, "Customer requested refund: damaged item", idempotencyKey),
                        maxRetries,
                        toolsInvoked
                );
            }

            // Step 5: Model Synthesis Call
            Instant modelStart2 = Instant.now();
            simulateModelLatency(180);
            Instant modelEnd2 = Instant.now();

            String synthesisPrompt = "Synthesize final response for customer based on order " + extractedOrderId;
            String finalAnswer = "Hello, thank you for reaching out to customer support. " +
                    "I have checked your order " + extractedOrderId + ". " +
                    "The item (" + productId + ") is in stock in our warehouse. " +
                    (requestRefund ? "Per your request, a full refund of $" + amount + " has been initiated safely (simulated mode). Reference ID is confirmed." : "Your order is confirmed.") +
                    " Please let us know if you need any further assistance!";

            traceRecorder.recordModelCall(
                    ctx,
                    "model.response_synthesis",
                    "gpt-4o",
                    320,
                    95,
                    modelStart2,
                    modelEnd2,
                    synthesisPrompt,
                    finalAnswer,
                    "OK",
                    null
            );

            responseBuilder.append(finalAnswer);

        } catch (Exception e) {
            log.error("Workflow failed on trace {}: {}", ctx.traceId(), e.getMessage());
            currentStatus = "ERROR";
            fatalError = e.getMessage();
            responseBuilder.append("I encountered an error while processing your request: ").append(e.getMessage());
        } finally {
            traceRecorder.finishTrace(ctx, responseBuilder.toString(), currentStatus, fatalError);
        }

        return AgentExecutionResult.builder()
                .traceId(ctx.traceId())
                .finalResponse(responseBuilder.toString())
                .status(currentStatus)
                .toolCallsMade(toolsInvoked)
                .totalDurationMs(Instant.now().toEpochMilli() - ctx.startTime().toEpochMilli())
                .errorMessage(fatalError)
                .build();
    }

    private String executeToolWithRetry(
            TraceContext ctx,
            String toolName,
            String inputPayload,
            ToolSupplier supplier,
            int maxRetries,
            List<String> toolsInvoked
    ) {
        toolsInvoked.add(toolName);
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= maxRetries) {
            attempts++;
            Instant start = Instant.now();
            try {
                String result = supplier.get();
                Instant end = Instant.now();
                traceRecorder.recordToolExecution(ctx, toolName, inputPayload, result, start, end, "OK", null);
                return result;
            } catch (Exception e) {
                Instant end = Instant.now();
                lastException = e;
                String errorMsg = "Attempt " + attempts + " failed: " + e.getMessage();
                traceRecorder.recordToolExecution(ctx, toolName, inputPayload, null, start, end, "ERROR", errorMsg);

                if (attempts <= maxRetries) {
                    log.warn("Retrying tool {} after error (attempt {}/{})...", toolName, attempts, maxRetries);
                    try {
                        Thread.sleep(100L * attempts); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("Tool execution failed after " + attempts + " attempts for " + toolName + ": " + (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    private String extractOrderId(String text) {
        if (text == null) return "ORD-9821";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("ORD-\\d+").matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "ORD-9821";
    }

    private void simulateModelLatency(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface ToolSupplier {
        String get() throws Exception;
    }
}
