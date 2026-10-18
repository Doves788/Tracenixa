package com.tracenixa.platform.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.fault.context.FaultContext;
import com.tracenixa.platform.fault.model.FaultConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerSupportTools {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Idempotency cache for destructive operations (e.g., refunds)
    private final Map<String, RefundRecord> processedRefunds = new ConcurrentHashMap<>();

    // Mock database of orders
    private final Map<String, OrderRecord> orderDatabase = new ConcurrentHashMap<>();
    // Mock database of product inventory
    private final Map<String, Integer> inventoryDatabase = new ConcurrentHashMap<>();

    {
        // Seed mock orders
        orderDatabase.put("ORD-9821", new OrderRecord("ORD-9821", "CUST-1001", "DELIVERED", 149.99, "PROD-WIRELESS-HEADPHONES", Instant.now().minusSeconds(86400 * 3)));
        orderDatabase.put("ORD-1044", new OrderRecord("ORD-1044", "CUST-1002", "IN_TRANSIT", 49.50, "PROD-USB-C-DOCK", Instant.now().minusSeconds(86400)));
        orderDatabase.put("ORD-7732", new OrderRecord("ORD-7732", "CUST-1003", "PROCESSING", 899.00, "PROD-4K-MONITOR", Instant.now().minusSeconds(3600)));

        // Seed mock inventory
        inventoryDatabase.put("PROD-WIRELESS-HEADPHONES", 12);
        inventoryDatabase.put("PROD-USB-C-DOCK", 0); // Out of stock
        inventoryDatabase.put("PROD-4K-MONITOR", 5);
    }

    public record OrderRecord(String orderId, String customerId, String status, double totalAmount, String productId, Instant orderDate) {}
    public record RefundRecord(String refundId, String orderId, double amount, String reason, String idempotencyKey, Instant processedAt, boolean simulated) {}

    /**
     * Tool: getOrderStatus
     */
    public String getOrderStatus(String orderId) {
        checkAndApplyFault("getOrderStatus");

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }

        // Simulate realistic network latency (40ms)
        simulateLatency(40);

        OrderRecord order = orderDatabase.get(orderId.trim());
        if (order == null) {
            return "{\"error\": \"Order not found: " + orderId + "\"}";
        }

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.orderId());
            map.put("customerId", order.customerId());
            map.put("status", order.status());
            map.put("totalAmount", order.totalAmount());
            map.put("productId", order.productId());
            map.put("orderDate", order.orderDate().toString());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Tool: checkInventory
     */
    public String checkInventory(String productId) {
        checkAndApplyFault("checkInventory");

        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }

        simulateLatency(50);

        Integer inStock = inventoryDatabase.getOrDefault(productId.trim(), 0);
        boolean available = inStock > 0;

        try {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", productId);
            map.put("unitsInStock", inStock);
            map.put("isAvailable", available);
            map.put("restockDate", available ? "In Stock" : "Expected in 3 days");
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Tool: refundOrder
     * Destructive operation: simulated by default with strict idempotency protection.
     */
    public String refundOrder(String orderId, Double amount, String reason, String idempotencyKey) {
        checkAndApplyFault("refundOrder");

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("refund amount must be greater than 0");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required for refund operation");
        }

        simulateLatency(80);

        // Idempotency check: if already processed with this key, return existing record
        if (processedRefunds.containsKey(idempotencyKey)) {
            RefundRecord existing = processedRefunds.get(idempotencyKey);
            log.info("Idempotent refund request detected for key {}. Returning existing record.", idempotencyKey);
            try {
                Map<String, Object> res = new HashMap<>();
                res.put("refundId", existing.refundId());
                res.put("orderId", existing.orderId());
                res.put("amount", existing.amount());
                res.put("status", "SUCCESS_IDEMPOTENT_REPLAY");
                res.put("simulated", existing.simulated());
                res.put("message", "Refund previously processed. Idempotent response returned.");
                return objectMapper.writeValueAsString(res);
            } catch (Exception e) {
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }

        // Create new simulated refund
        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        RefundRecord newRefund = new RefundRecord(refundId, orderId, amount, reason, idempotencyKey, Instant.now(), true);
        processedRefunds.put(idempotencyKey, newRefund);

        try {
            Map<String, Object> res = new HashMap<>();
            res.put("refundId", refundId);
            res.put("orderId", orderId);
            res.put("amount", amount);
            res.put("status", "REFUND_SIMULATED_SUCCESS");
            res.put("simulated", true);
            res.put("idempotencyKey", idempotencyKey);
            res.put("processedAt", newRefund.processedAt().toString());
            res.put("reason", reason);
            return objectMapper.writeValueAsString(res);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private void checkAndApplyFault(String toolName) {
        FaultConfig fault = FaultContext.getActiveFault();
        if (fault == null) {
            return;
        }

        if (fault.getTargetTool() == null || fault.getTargetTool().equalsIgnoreCase(toolName) || fault.getTargetTool().equals("*")) {
            log.warn("Applying fault injection [{}] to tool [{}]", fault.getFaultType(), toolName);

            switch (fault.getFaultType().toUpperCase()) {
                case "TIMEOUT" -> {
                    long delay = fault.getDelayMs() != null ? fault.getDelayMs() : 3000L;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("TOOL_TIMEOUT: Connection timed out while invoking " + toolName + " after " + delay + "ms");
                }
                case "RATE_LIMIT" -> {
                    throw new RuntimeException("TOOL_RATE_LIMIT_429: HTTP 429 Too Many Requests: Rate limit exceeded for upstream tool " + toolName);
                }
                case "TRANSIENT_500" -> {
                    throw new RuntimeException("TOOL_TRANSIENT_500: HTTP 500 Internal Server Error (Transient upstream dependency error in " + toolName + ")");
                }
                case "MALFORMED_JSON" -> {
                    throw new RuntimeException("TOOL_MALFORMED_JSON: Received invalid JSON response payload from " + toolName + ": { error_unclosed_json: ");
                }
                case "DUPLICATE_EVENT" -> {
                    log.info("Simulating duplicate event delivery for tool {}", toolName);
                }
                default -> {}
            }
        }
    }

    private void simulateLatency(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
