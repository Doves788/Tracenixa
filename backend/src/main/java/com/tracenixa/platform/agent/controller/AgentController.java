package com.tracenixa.platform.agent.controller;

import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.agent.service.CustomerSupportAgent.AgentExecutionResult;
import com.tracenixa.platform.fault.context.FaultContext;
import com.tracenixa.platform.fault.model.FaultConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/agent")
@RequiredArgsConstructor
@Tag(name = "Sample AI Agent", description = "Customer support agent execution and resilience testing")
public class AgentController {

    private final CustomerSupportAgent agent;

    @Getter
    @Setter
    public static class ChatRequest {
        private String prompt = "Please check order ORD-9821 and process a refund because the headphones arrived damaged.";
        private int maxRetries = 1;
    }

    @Getter
    @Setter
    public static class FaultSimulationRequest {
        private String prompt = "Check inventory for my order ORD-9821.";
        private String targetTool = "checkInventory";
        private String faultType = "TIMEOUT"; // TIMEOUT, RATE_LIMIT, MALFORMED_JSON, TRANSIENT_500
        private Long delayMs = 1500L;
        private int maxRetries = 0; // Set to 0 to simulate unhandled failure, or >0 to test retry resilience
    }

    @PostMapping("/chat")
    @Operation(summary = "Execute customer support workflow and record distributed trace")
    public ResponseEntity<AgentExecutionResult> chat(
            @PathVariable String projectId,
            @RequestBody(required = false) ChatRequest request) {
        String prompt = (request != null && request.getPrompt() != null && !request.getPrompt().isBlank())
                ? request.getPrompt()
                : "Please check order ORD-9821 and process a refund because the headphones arrived damaged.";
        int retries = request != null ? request.getMaxRetries() : 1;
        return ResponseEntity.ok(agent.runWorkflow(projectId, prompt, retries));
    }

    @PostMapping("/simulate-fault")
    @Operation(summary = "Inject controlled fault into tool execution to demonstrate failure reproduction and resilience")
    public ResponseEntity<AgentExecutionResult> simulateFault(
            @PathVariable String projectId,
            @RequestBody FaultSimulationRequest request) {
        FaultConfig fault = FaultConfig.builder()
                .targetTool(request.getTargetTool())
                .faultType(request.getFaultType())
                .delayMs(request.getDelayMs())
                .build();

        try {
            FaultContext.setActiveFault(fault);
            return ResponseEntity.ok(agent.runWorkflow(projectId, request.getPrompt(), request.getMaxRetries()));
        } finally {
            FaultContext.clear();
        }
    }
}
