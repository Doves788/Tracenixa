package com.tracenixa.platform;

import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.replay.dto.ReplayDtos.CreateReplayRequest;
import com.tracenixa.platform.replay.dto.ReplayDtos.ReplayDetailResponse;
import com.tracenixa.platform.replay.service.ReplayEngineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RecordAndReplayEngineTest {

    @Autowired
    private ReplayEngineService replayEngineService;

    @Autowired
    private CustomerSupportAgent agent;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Recorded replay reproduces execution sequence and generates step-level diff")
    void testRecordedReplayEngine() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Generate live source trace
        var sourceResult = agent.runWorkflow(project.getId(), "Check order ORD-9821 and process refund for damaged item", 1);
        assertThat(sourceResult.getStatus()).isEqualTo("OK");

        // 2. Launch recorded replay session
        var sessionRes = replayEngineService.createReplaySession(
                project.getId(),
                "test-user",
                CreateReplayRequest.builder()
                        .sourceTraceId(sourceResult.getTraceId())
                        .replayMode("RECORDED")
                        .build()
        );

        assertThat(sessionRes.getId()).isNotBlank();

        // 3. Inspect replay details and side-by-side step comparison
        ReplayDetailResponse detail = replayEngineService.getReplayDetail(project.getId(), sessionRes.getId());
        assertThat(detail.getSession().getStatus()).isIn("RUNNING", "COMPLETED");
        assertThat(detail.getStepComparisons()).isNotEmpty();

        var firstStep = detail.getStepComparisons().get(0);
        assertThat(firstStep.getOperationName()).isNotBlank();
        assertThat(firstStep.getStatus()).isIn("MATCHED", "DIVERGED", "INJECTED_FAULT");
    }
}
