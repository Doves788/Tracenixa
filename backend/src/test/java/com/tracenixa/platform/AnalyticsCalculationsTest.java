package com.tracenixa.platform;

import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.analytics.service.AnalyticsService;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
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
class AnalyticsCalculationsTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CustomerSupportAgent agent;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @DisplayName("Analytics service aggregates actual stored traces into p50/p95, model costs, and tool metrics")
    void testAnalyticsCalculations() {
        Project project = projectRepository.findAll().stream().findFirst().orElseThrow();

        // Run agent workflow to ensure data
        agent.runWorkflow(project.getId(), "Check order ORD-9821 status", 1);

        var overview = analyticsService.getOverviewStats(project.getId());
        assertThat(overview.getTotalTraces()).isGreaterThan(0);
        assertThat(overview.getSuccessRate()).isGreaterThanOrEqualTo(0.0);
        assertThat(overview.getP50LatencyMs()).isGreaterThan(0);
        assertThat(overview.getTotalTokens()).isGreaterThan(0);

        var models = analyticsService.getModelCosts(project.getId());
        assertThat(models).isNotEmpty();
        assertThat(models.get(0).getModelName()).isNotBlank();

        var tools = analyticsService.getToolPerformance(project.getId());
        assertThat(tools).isNotEmpty();

        var latencyHist = analyticsService.getLatencyHistogram(project.getId());
        assertThat(latencyHist).hasSize(5);

        var trends = analyticsService.getDailyTrends(project.getId());
        assertThat(trends).hasSize(7);
    }
}
