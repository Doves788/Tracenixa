package com.tracenixa.platform.evaluation.controller;

import com.tracenixa.platform.evaluation.dto.EvaluationDtos.*;
import com.tracenixa.platform.evaluation.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Evaluations & Regression", description = "AI benchmark datasets, LLM-as-a-judge scoring, and CI gates")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/projects/{projectId}/evaluations/datasets")
    @Operation(summary = "Create evaluation benchmark dataset")
    public ResponseEntity<DatasetResponse> createDataset(
            @PathVariable String projectId,
            @Valid @RequestBody CreateDatasetRequest request) {
        return ResponseEntity.ok(evaluationService.createDataset(projectId, request));
    }

    @GetMapping("/projects/{projectId}/evaluations/datasets")
    @Operation(summary = "List all evaluation datasets for project")
    public ResponseEntity<List<DatasetResponse>> getDatasets(@PathVariable String projectId) {
        return ResponseEntity.ok(evaluationService.getDatasets(projectId));
    }

    @GetMapping("/projects/{projectId}/evaluations/datasets/{datasetId}")
    @Operation(summary = "Get dataset by ID with test cases")
    public ResponseEntity<DatasetResponse> getDataset(
            @PathVariable String projectId,
            @PathVariable String datasetId) {
        return ResponseEntity.ok(evaluationService.getDatasetById(projectId, datasetId));
    }

    @PostMapping("/projects/{projectId}/evaluations/runs")
    @Operation(summary = "Execute evaluation run on dataset with deterministic & LLM judge scoring")
    public ResponseEntity<EvaluationRunResponse> runEvaluation(
            @PathVariable String projectId,
            @Valid @RequestBody TriggerEvaluationRequest request) {
        return ResponseEntity.ok(evaluationService.runEvaluation(projectId, request));
    }

    @GetMapping("/projects/{projectId}/evaluations/runs")
    @Operation(summary = "List all evaluation runs with scores and metrics")
    public ResponseEntity<Page<EvaluationRunResponse>> listRuns(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(evaluationService.listEvaluationRuns(projectId, page, size));
    }

    @GetMapping("/projects/{projectId}/evaluations/runs/{runId}")
    @Operation(summary = "Get detailed results of an evaluation run")
    public ResponseEntity<EvaluationRunResponse> getRun(
            @PathVariable String projectId,
            @PathVariable String runId) {
        return ResponseEntity.ok(evaluationService.getEvaluationRun(projectId, runId));
    }

    @PostMapping("/evaluations/ci-check")
    @Operation(summary = "CI-compatible endpoint: verifies regression thresholds, returns 200 or 412 on failure")
    public ResponseEntity<CiCheckResponse> ciCheck(@Valid @RequestBody CiCheckRequest request) {
        CiCheckResponse res = evaluationService.executeCiCheck(request);
        if (!res.isPassed()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(res);
        }
        return ResponseEntity.ok(res);
    }

    @PostMapping("/projects/{projectId}/regression-tests")
    @Operation(summary = "Promote a failed trace or test case into a persistent regression test")
    public ResponseEntity<RegressionTestResponse> createRegressionTest(
            @PathVariable String projectId,
            @Valid @RequestBody CreateRegressionTestRequest request) {
        return ResponseEntity.ok(evaluationService.createRegressionTest(projectId, request));
    }

    @GetMapping("/projects/{projectId}/regression-tests")
    @Operation(summary = "List all active regression tests for project")
    public ResponseEntity<List<RegressionTestResponse>> getRegressionTests(@PathVariable String projectId) {
        return ResponseEntity.ok(evaluationService.getRegressionTests(projectId));
    }

    @PostMapping("/projects/{projectId}/regression-tests/{testId}/run")
    @Operation(summary = "Execute a single regression test to verify fix")
    public ResponseEntity<RegressionTestResponse> runRegressionTest(
            @PathVariable String projectId,
            @PathVariable String testId) {
        return ResponseEntity.ok(evaluationService.runSingleRegressionTest(projectId, testId));
    }
}
