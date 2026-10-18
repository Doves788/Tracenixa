package com.tracenixa.platform.fault.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaultConfig {
    private String targetTool; // e.g. "checkInventory", "getOrderStatus", "refundOrder", "*"
    private String faultType;  // TIMEOUT, RATE_LIMIT, MALFORMED_JSON, TRANSIENT_500, DUPLICATE_EVENT
    private Long delayMs;      // e.g. 3000
    private String errorMessage;
    private Integer failureRatePercentage; // 1-100%
}
