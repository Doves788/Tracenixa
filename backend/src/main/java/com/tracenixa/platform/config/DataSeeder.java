package com.tracenixa.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.apikey.model.ApiKey;
import com.tracenixa.platform.apikey.repository.ApiKeyRepository;
import com.tracenixa.platform.apikey.util.ApiKeyHasher;
import com.tracenixa.platform.auth.model.User;
import com.tracenixa.platform.auth.repository.UserRepository;
import com.tracenixa.platform.evaluation.dto.EvaluationDtos.TriggerEvaluationRequest;
import com.tracenixa.platform.evaluation.service.EvaluationService;
import com.tracenixa.platform.fault.context.FaultContext;
import com.tracenixa.platform.fault.model.FaultConfig;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.replay.dto.ReplayDtos.CreateReplayRequest;
import com.tracenixa.platform.replay.service.ReplayEngineService;
import com.tracenixa.platform.trace.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerSupportAgent agent;
    private final ReplayEngineService replayEngineService;
    private final EvaluationService evaluationService;
    private final TraceRepository traceRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Seeding Tracenixa platform initial developer workspace and demonstration telemetry...");

        // 1. Seed Developer Account
        String userId = UUID.randomUUID().toString();
        User demoUser = User.builder()
                .id(userId)
                .email("developer@tracenixa.io")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Alex Mercer (Senior AI Engineer)")
                .role("ADMIN")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(demoUser);

        // 2. Seed Default Project
        String projectId = UUID.randomUUID().toString();
        Project demoProject = Project.builder()
                .id(projectId)
                .name("Tracenixa Customer Support Agent")
                .slug("support-agent-prod")
                .description("Customer support automation agent for order tracking, inventory validation, and safe simulated refunds.")
                .owner(demoUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        projectRepository.save(demoProject);

        // 3. Seed Live API Key
        String plaintextKey = "trx_live_demo1234567890abcdef12345678";
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID().toString())
                .project(demoProject)
                .name("Production Gateway Key")
                .keyPrefix(ApiKeyHasher.extractPrefix(plaintextKey))
                .keyHash(ApiKeyHasher.hashKey(plaintextKey))
                .createdBy(userId)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
        apiKeyRepository.save(apiKey);

        // 4. Generate Real Executions via CustomerSupportAgent
        log.info("Generating demonstration agent traces...");
        // Trace 1: Successful refund workflow
        var res1 = agent.runWorkflow(projectId, "I received order ORD-9821, but the wireless headphones arrived damaged. Please issue a refund.", 1);

        // Trace 2: Status check workflow
        var res2 = agent.runWorkflow(projectId, "Could you please check the status for order ORD-1044?", 1);

        // Trace 3: Inventory check workflow
        var res3 = agent.runWorkflow(projectId, "Is PROD-4K-MONITOR in stock for order ORD-7732?", 1);

        // Trace 4: Controlled fault injection failure (Timeout on checkInventory)
        try {
            FaultConfig timeoutFault = FaultConfig.builder()
                    .targetTool("checkInventory")
                    .faultType("TIMEOUT")
                    .delayMs(200L)
                    .build();
            FaultContext.setActiveFault(timeoutFault);
            agent.runWorkflow(projectId, "Please inspect stock for order ORD-9821 before processing a refund.", 0);
        } catch (Exception ignored) {
        } finally {
            FaultContext.clear();
        }

        // 5. Seed Recorded Replay Session based on Trace 1
        log.info("Generating demonstration replay session...");
        try {
            replayEngineService.createReplaySession(projectId, userId, CreateReplayRequest.builder()
                    .sourceTraceId(res1.getTraceId())
                    .replayMode("RECORDED")
                    .build());
        } catch (Exception e) {
            log.warn("Failed to auto-seed replay session: {}", e.getMessage());
        }

        // 6. Seed Baseline Evaluation Run
        log.info("Running baseline evaluation suite...");
        try {
            var datasets = evaluationService.getDatasets(projectId);
            if (!datasets.isEmpty()) {
                evaluationService.runEvaluation(projectId, TriggerEvaluationRequest.builder()
                        .datasetId(datasets.get(0).getId())
                        .name("Baseline Benchmark v1.0")
                        .agentVersion("v1.0-baseline")
                        .modelIdentifier("gpt-4o")
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to auto-seed evaluation run: {}", e.getMessage());
        }

        log.info("Tracenixa demonstration workspace initialized successfully!");
        log.info("Default Login -> Email: developer@tracenixa.io | Password: Password123!");
        log.info("Default API Key -> {}", plaintextKey);
    }
}
