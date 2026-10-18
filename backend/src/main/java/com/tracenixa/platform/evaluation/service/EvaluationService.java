package com.tracenixa.platform.evaluation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.agent.service.CustomerSupportAgent.AgentExecutionResult;
import com.tracenixa.platform.evaluation.dto.EvaluationDtos.*;
import com.tracenixa.platform.evaluation.model.EvaluationDataset;
import com.tracenixa.platform.evaluation.model.EvaluationRun;
import com.tracenixa.platform.evaluation.model.RegressionTest;
import com.tracenixa.platform.evaluation.repository.EvaluationDatasetRepository;
import com.tracenixa.platform.evaluation.repository.EvaluationRunRepository;
import com.tracenixa.platform.evaluation.repository.RegressionTestRepository;
import com.tracenixa.platform.evaluation.service.LlmJudgeEvaluator.Judgment;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationDatasetRepository datasetRepository;
    private final EvaluationRunRepository runRepository;
    private final RegressionTestRepository regressionRepository;
    private final ProjectRepository projectRepository;
    private final CustomerSupportAgent agent;
    private final LlmJudgeEvaluator llmJudge;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DatasetResponse createDataset(String projectId, CreateDatasetRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String testCasesJson = "[]";
        try {
            testCasesJson = objectMapper.writeValueAsString(request.getTestCases() != null ? request.getTestCases() : Collections.emptyList());
        } catch (Exception ignored) {}

        EvaluationDataset dataset = EvaluationDataset.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .name(request.getName().trim())
                .description(request.getDescription())
                .version(request.getVersion() != null ? request.getVersion() : "v1.0")
                .testCases(testCasesJson)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        datasetRepository.save(dataset);
        return toDatasetResponse(dataset);
    }

    public List<DatasetResponse> getDatasets(String projectId) {
        ensureDefaultDatasetExists(projectId);
        return datasetRepository.findByProjectId(projectId).stream()
                .map(this::toDatasetResponse)
                .collect(Collectors.toList());
    }

    public DatasetResponse getDatasetById(String projectId, String datasetId) {
        EvaluationDataset dataset = datasetRepository.findByProjectIdAndId(projectId, datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + datasetId));
        return toDatasetResponse(dataset);
    }

    @Transactional
    public EvaluationRunResponse runEvaluation(String projectId, TriggerEvaluationRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        EvaluationDataset dataset = datasetRepository.findByProjectIdAndId(projectId, request.getDatasetId())
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found: " + request.getDatasetId()));

        List<TestCaseDto> testCases = parseTestCases(dataset.getTestCases());

        String runId = UUID.randomUUID().toString();
        EvaluationRun run = EvaluationRun.builder()
                .id(runId)
                .project(project)
                .dataset(dataset)
                .name(request.getName() != null ? request.getName() : "Evaluation Run - " + dataset.getName())
                .agentVersion(request.getAgentVersion() != null ? request.getAgentVersion() : "v1.0-prod")
                .modelIdentifier(request.getModelIdentifier() != null ? request.getModelIdentifier() : "gpt-4o")
                .status("RUNNING")
                .totalCases(testCases.size())
                .createdAt(Instant.now())
                .build();

        runRepository.save(run);

        // Execute test cases
        List<TestCaseResultDto> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        double sumFaithfulness = 0;
        double sumRelevance = 0;
        double sumAccuracy = 0;

        for (TestCaseDto tc : testCases) {
            AgentExecutionResult agentRes = agent.runWorkflow(projectId, tc.getInput(), 1);

            // Deterministic tool matching
            boolean toolsMatch = tc.getExpectedTools() == null || tc.getExpectedTools().isEmpty()
                    || new HashSet<>(agentRes.getToolCallsMade()).containsAll(tc.getExpectedTools());

            // Ground-truth qualitative judgment
            Judgment judgment = llmJudge.evaluateResponse(
                    tc.getInput(),
                    agentRes.getFinalResponse(),
                    agentRes.getToolCallsMade(),
                    tc.getRequiredKeywords()
            );

            boolean passedCase = "OK".equals(agentRes.getStatus()) && toolsMatch && judgment.faithfulnessScore() >= 60.0;
            if (passedCase) passed++; else failed++;

            double deterministicScore = ("OK".equals(agentRes.getStatus()) ? 50.0 : 0.0) + (toolsMatch ? 50.0 : 0.0);

            sumFaithfulness += judgment.faithfulnessScore();
            sumRelevance += judgment.relevanceScore();
            sumAccuracy += toolsMatch ? 100.0 : 0.0;

            results.add(TestCaseResultDto.builder()
                    .testCaseId(tc.getId() != null ? tc.getId() : UUID.randomUUID().toString())
                    .testCaseName(tc.getName() != null ? tc.getName() : "Case")
                    .input(tc.getInput())
                    .output(agentRes.getFinalResponse())
                    .toolsInvoked(agentRes.getToolCallsMade())
                    .toolsMatched(toolsMatch)
                    .assertionsPassed("OK".equals(agentRes.getStatus()))
                    .deterministicScore(deterministicScore)
                    .faithfulnessScore(judgment.faithfulnessScore())
                    .relevanceScore(judgment.relevanceScore())
                    .status(passedCase ? "PASSED" : "FAILED")
                    .failureReason(passedCase ? null : judgment.reasoning())
                    .build());
        }

        int count = Math.max(1, testCases.size());
        double avgFaith = sumFaithfulness / count;
        double avgRel = sumRelevance / count;
        double avgAcc = sumAccuracy / count;
        double overallScore = (avgFaith * 0.4) + (avgRel * 0.3) + (avgAcc * 0.3);

        try {
            run.setDetailedResults(objectMapper.writeValueAsString(results));
        } catch (Exception ignored) {}

        run.setStatus("COMPLETED");
        run.setPassedCases(passed);
        run.setFailedCases(failed);
        run.setAverageScore(BigDecimal.valueOf(overallScore).setScale(2, RoundingMode.HALF_UP));
        run.setFaithfulnessScore(BigDecimal.valueOf(avgFaith).setScale(2, RoundingMode.HALF_UP));
        run.setRelevanceScore(BigDecimal.valueOf(avgRel).setScale(2, RoundingMode.HALF_UP));
        run.setToolAccuracyScore(BigDecimal.valueOf(avgAcc).setScale(2, RoundingMode.HALF_UP));
        run.setCompletedAt(Instant.now());

        runRepository.save(run);
        return toRunResponse(run, results);
    }

    public Page<EvaluationRunResponse> listEvaluationRuns(String projectId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return runRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pr)
                .map(r -> toRunResponse(r, parseResults(r.getDetailedResults())));
    }

    public EvaluationRunResponse getEvaluationRun(String projectId, String runId) {
        EvaluationRun run = runRepository.findByProjectIdAndId(projectId, runId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation run not found: " + runId));
        return toRunResponse(run, parseResults(run.getDetailedResults()));
    }

    public CiCheckResponse executeCiCheck(CiCheckRequest request) {
        ensureDefaultDatasetExists(request.getProjectId());

        String targetDatasetId = request.getDatasetId();
        if (targetDatasetId == null || targetDatasetId.isBlank()) {
            targetDatasetId = datasetRepository.findByProjectId(request.getProjectId()).get(0).getId();
        }

        TriggerEvaluationRequest trigger = TriggerEvaluationRequest.builder()
                .datasetId(targetDatasetId)
                .name("CI Automated Evaluation Pipeline Check")
                .agentVersion("ci-build")
                .modelIdentifier("gpt-4o")
                .build();

        EvaluationRunResponse run = runEvaluation(request.getProjectId(), trigger);

        double passRate = (run.getPassedCases() * 100.0) / Math.max(1, run.getTotalCases());
        double faithfulness = run.getFaithfulnessScore().doubleValue();

        boolean passed = passRate >= request.getMinPassRate() && faithfulness >= request.getMinFaithfulness();

        String msg = passed
                ? String.format("CI Passed: Pass rate %.1f%% (min %.1f%%), Faithfulness %.1f%% (min %.1f%%)",
                passRate, request.getMinPassRate(), faithfulness, request.getMinFaithfulness())
                : String.format("CI FAILED: Threshold violation. Pass rate %.1f%% (min %.1f%%), Faithfulness %.1f%% (min %.1f%%)",
                passRate, request.getMinPassRate(), faithfulness, request.getMinFaithfulness());

        return CiCheckResponse.builder()
                .passed(passed)
                .exitCode(passed ? 0 : 1)
                .message(msg)
                .actualPassRate(passRate)
                .actualFaithfulness(faithfulness)
                .runId(run.getId())
                .evaluatedAt(Instant.now())
                .build();
    }

    @Transactional
    public RegressionTestResponse createRegressionTest(String projectId, CreateRegressionTestRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        String toolsJson = "[]";
        String assertionsJson = "[]";
        try {
            if (request.getExpectedTools() != null) toolsJson = objectMapper.writeValueAsString(request.getExpectedTools());
            if (request.getExpectedAssertions() != null) assertionsJson = objectMapper.writeValueAsString(request.getExpectedAssertions());
        } catch (Exception ignored) {}

        RegressionTest reg = RegressionTest.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .name(request.getName().trim())
                .description(request.getDescription())
                .sourceTraceId(request.getSourceTraceId())
                .inputPayload(request.getInputPayload())
                .expectedTools(toolsJson)
                .expectedAssertions(assertionsJson)
                .toleranceMs(request.getToleranceMs() != null ? request.getToleranceMs() : 5000L)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        regressionRepository.save(reg);
        return toRegressionResponse(reg);
    }

    public List<RegressionTestResponse> getRegressionTests(String projectId) {
        return regressionRepository.findByProjectIdAndStatus(projectId, "ACTIVE").stream()
                .map(this::toRegressionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegressionTestResponse runSingleRegressionTest(String projectId, String testId) {
        RegressionTest test = regressionRepository.findByProjectIdAndId(projectId, testId)
                .orElseThrow(() -> new IllegalArgumentException("Regression test not found: " + testId));

        AgentExecutionResult result = agent.runWorkflow(projectId, test.getInputPayload(), 1);
        boolean passed = "OK".equals(result.getStatus());

        test.setLastRunAt(Instant.now());
        test.setLastRunStatus(passed ? "PASSED" : "FAILED");
        regressionRepository.save(test);

        return toRegressionResponse(test);
    }

    private void ensureDefaultDatasetExists(String projectId) {
        List<EvaluationDataset> existing = datasetRepository.findByProjectId(projectId);
        if (!existing.isEmpty()) return;

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        List<TestCaseDto> defaultCases = List.of(
                TestCaseDto.builder()
                        .id("tc-1")
                        .name("Damaged Item Refund & Replacement")
                        .input("I received order ORD-9821, but the item arrived damaged. Can I get a replacement or refund?")
                        .expectedTools(List.of("getOrderStatus", "checkInventory", "refundOrder"))
                        .requiredKeywords(List.of("refund", "stock"))
                        .expectedResolution("REFUND")
                        .build(),
                TestCaseDto.builder()
                        .id("tc-2")
                        .name("Order Status Lookup Only")
                        .input("Can you tell me the current shipping status of order ORD-1044?")
                        .expectedTools(List.of("getOrderStatus"))
                        .requiredKeywords(List.of("order", "status"))
                        .expectedResolution("STATUS_LOOKUP")
                        .build(),
                TestCaseDto.builder()
                        .id("tc-3")
                        .name("Inventory Availability Check")
                        .input("Please verify if there is stock available for PROD-4K-MONITOR.")
                        .expectedTools(List.of("checkInventory"))
                        .requiredKeywords(List.of("stock", "warehouse"))
                        .expectedResolution("REPLACEMENT")
                        .build(),
                TestCaseDto.builder()
                        .id("tc-4")
                        .name("Delayed Order Resolution")
                        .input("Order ORD-7732 was delayed. Please check the order and issue a refund.")
                        .expectedTools(List.of("getOrderStatus", "refundOrder"))
                        .requiredKeywords(List.of("refund"))
                        .expectedResolution("REFUND")
                        .build()
        );

        try {
            EvaluationDataset ds = EvaluationDataset.builder()
                    .id(UUID.randomUUID().toString())
                    .project(project)
                    .name("Customer Support Reliability Benchmark v1")
                    .description("Core test suite covering order inquiries, inventory checks, refund workflows, and edge conditions")
                    .version("v1.0")
                    .testCases(objectMapper.writeValueAsString(defaultCases))
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            datasetRepository.save(ds);
        } catch (Exception ignored) {}
    }

    private List<TestCaseDto> parseTestCases(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<TestCaseDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<TestCaseResultDto> parseResults(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<TestCaseResultDto>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private DatasetResponse toDatasetResponse(EvaluationDataset d) {
        List<TestCaseDto> cases = parseTestCases(d.getTestCases());
        return DatasetResponse.builder()
                .id(d.getId())
                .projectId(d.getProject().getId())
                .name(d.getName())
                .description(d.getDescription())
                .version(d.getVersion())
                .testCases(cases)
                .testCaseCount(cases.size())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private EvaluationRunResponse toRunResponse(EvaluationRun r, List<TestCaseResultDto> results) {
        return EvaluationRunResponse.builder()
                .id(r.getId())
                .projectId(r.getProject().getId())
                .datasetId(r.getDataset().getId())
                .datasetName(r.getDataset().getName())
                .name(r.getName())
                .agentVersion(r.getAgentVersion())
                .modelIdentifier(r.getModelIdentifier())
                .status(r.getStatus())
                .totalCases(r.getTotalCases())
                .passedCases(r.getPassedCases())
                .failedCases(r.getFailedCases())
                .averageScore(r.getAverageScore())
                .faithfulnessScore(r.getFaithfulnessScore())
                .relevanceScore(r.getRelevanceScore())
                .toolAccuracyScore(r.getToolAccuracyScore())
                .results(results)
                .createdAt(r.getCreatedAt())
                .completedAt(r.getCompletedAt())
                .build();
    }

    private RegressionTestResponse toRegressionResponse(RegressionTest t) {
        List<String> tools = Collections.emptyList();
        List<String> assertions = Collections.emptyList();
        try {
            if (t.getExpectedTools() != null) tools = objectMapper.readValue(t.getExpectedTools(), new TypeReference<List<String>>() {});
            if (t.getExpectedAssertions() != null) assertions = objectMapper.readValue(t.getExpectedAssertions(), new TypeReference<List<String>>() {});
        } catch (Exception ignored) {}

        return RegressionTestResponse.builder()
                .id(t.getId())
                .projectId(t.getProject().getId())
                .name(t.getName())
                .description(t.getDescription())
                .sourceTraceId(t.getSourceTraceId())
                .inputPayload(t.getInputPayload())
                .expectedTools(tools)
                .expectedAssertions(assertions)
                .toleranceMs(t.getToleranceMs())
                .status(t.getStatus())
                .lastRunAt(t.getLastRunAt())
                .lastRunStatus(t.getLastRunStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
