package com.tracenixa.platform;

import com.tracenixa.platform.evaluation.dto.EvaluationDtos.*;
import com.tracenixa.platform.evaluation.service.EvaluationService;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EvaluationAndCiCheckTest {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Evaluation suite computes faithfulness, relevance, and tool accuracy scores")
    void testEvaluationSuite() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();
        var datasets = evaluationService.getDatasets(project.getId());
        assertThat(datasets).isNotEmpty();

        var dataset = datasets.get(0);
        EvaluationRunResponse run = evaluationService.runEvaluation(project.getId(), TriggerEvaluationRequest.builder()
                .datasetId(dataset.getId())
                .name("Integration Test Run")
                .agentVersion("v2.0-candidate")
                .modelIdentifier("gpt-4o")
                .build());

        assertThat(run.getStatus()).isEqualTo("COMPLETED");
        assertThat(run.getTotalCases()).isGreaterThan(0);
        assertThat(run.getAverageScore().doubleValue()).isGreaterThan(50.0);
        assertThat(run.getFaithfulnessScore().doubleValue()).isGreaterThan(60.0);
        assertThat(run.getResults()).isNotEmpty();
    }

    @Test
    @DisplayName("CI check enforces regression thresholds and returns pass or threshold violation")
    void testCiCheckThresholds() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        // High threshold that passes
        CiCheckResponse passRes = evaluationService.executeCiCheck(CiCheckRequest.builder()
                .projectId(project.getId())
                .minPassRate(50.0)
                .minFaithfulness(60.0)
                .build());

        assertThat(passRes.isPassed()).isTrue();
        assertThat(passRes.getExitCode()).isEqualTo(0);

        // Impossibly strict threshold (101%) that triggers failure gate
        CiCheckResponse failRes = evaluationService.executeCiCheck(CiCheckRequest.builder()
                .projectId(project.getId())
                .minPassRate(101.0)
                .minFaithfulness(101.0)
                .build());

        assertThat(failRes.isPassed()).isFalse();
        assertThat(failRes.getExitCode()).isEqualTo(1);
        assertThat(failRes.getMessage()).contains("CI FAILED: Threshold violation");
    }
}
