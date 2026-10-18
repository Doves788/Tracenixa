package com.tracenixa.platform.replay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracenixa.platform.agent.service.CustomerSupportAgent;
import com.tracenixa.platform.agent.service.CustomerSupportAgent.AgentExecutionResult;
import com.tracenixa.platform.fault.context.FaultContext;
import com.tracenixa.platform.fault.model.FaultConfig;
import com.tracenixa.platform.kafka.producer.EventPublisher;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import com.tracenixa.platform.replay.dto.ReplayDtos.*;
import com.tracenixa.platform.replay.model.RecordedEvent;
import com.tracenixa.platform.replay.model.ReplaySession;
import com.tracenixa.platform.replay.repository.RecordedEventRepository;
import com.tracenixa.platform.replay.repository.ReplaySessionRepository;
import com.tracenixa.platform.replay.sandbox.SandboxToolAdapter;
import com.tracenixa.platform.trace.model.Span;
import com.tracenixa.platform.trace.model.Trace;
import com.tracenixa.platform.trace.repository.SpanRepository;
import com.tracenixa.platform.trace.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayEngineService {

    private final ReplaySessionRepository replaySessionRepository;
    private final TraceRepository traceRepository;
    private final SpanRepository spanRepository;
    private final RecordedEventRepository recordedEventRepository;
    private final ProjectRepository projectRepository;
    private final CustomerSupportAgent agent;
    private final SandboxToolAdapter sandboxAdapter;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ReplaySessionResponse createReplaySession(String projectId, String userId, CreateReplayRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        Trace sourceTrace = traceRepository.findByProjectIdAndTraceId(projectId, request.getSourceTraceId())
                .orElseThrow(() -> new IllegalArgumentException("Source trace not found: " + request.getSourceTraceId()));

        String faultConfigJson = null;
        if (request.getFaultConfig() != null) {
            try {
                faultConfigJson = objectMapper.writeValueAsString(request.getFaultConfig());
            } catch (Exception ignored) {}
        }

        ReplaySession session = ReplaySession.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .sourceTraceId(sourceTrace.getTraceId())
                .replayMode(request.getReplayMode().toUpperCase())
                .status("PENDING")
                .faultConfig(faultConfigJson)
                .originalDurationMs(sourceTrace.getDurationMs())
                .stepsCount(0)
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();

        replaySessionRepository.save(session);

        // Publish async job
        eventPublisher.publishReplayRequested(
                session.getId(),
                projectId,
                sourceTrace.getTraceId(),
                session.getReplayMode(),
                faultConfigJson
        );

        // Execute async processing
        executeReplaySession(session.getId());

        return toSessionResponse(session);
    }

    @Async
    public void executeReplaySession(String sessionId) {
        ReplaySession session = replaySessionRepository.findById(sessionId).orElse(null);
        if (session == null || !"PENDING".equals(session.getStatus())) {
            return;
        }

        session.setStatus("RUNNING");
        replaySessionRepository.save(session);

        Instant startReplay = Instant.now();

        try {
            Trace sourceTrace = traceRepository.findByTraceId(session.getSourceTraceId())
                    .orElseThrow(() -> new IllegalStateException("Source trace not found: " + session.getSourceTraceId()));

            List<RecordedEvent> originalEvents = recordedEventRepository.findByTraceIdOrderByStepOrderAsc(session.getSourceTraceId());
            List<Span> originalSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(session.getSourceTraceId());

            FaultConfig faultConfig = null;
            if (session.getFaultConfig() != null && !session.getFaultConfig().isBlank()) {
                faultConfig = objectMapper.readValue(session.getFaultConfig(), FaultConfig.class);
            }

            AgentExecutionResult result;
            String prompt = sourceTrace.getInputPayload() != null ? sourceTrace.getInputPayload() : "Check order ORD-9821";

            if ("FAULT_INJECTION".equals(session.getReplayMode()) && faultConfig != null) {
                try {
                    FaultContext.setActiveFault(faultConfig);
                    // In fault injection replay, simulate unhandled failure (0 retries) or bounded retries based on config
                    int retries = faultConfig.getFailureRatePercentage() != null && faultConfig.getFailureRatePercentage() < 100 ? 1 : 0;
                    result = agent.runWorkflow(session.getProject().getId(), prompt, retries);
                } finally {
                    FaultContext.clear();
                }
            } else if ("RECORDED".equals(session.getReplayMode())) {
                // Register recorded events into sandbox adapter
                sandboxAdapter.registerRecordedEvents(session.getId(), originalEvents);
                try {
                    result = agent.runWorkflow(session.getProject().getId(), prompt, 1);
                } finally {
                    sandboxAdapter.cleanUpSession(session.getId());
                }
            } else {
                // LIVE mode: rerun against live agent
                result = agent.runWorkflow(session.getProject().getId(), prompt, 1);
            }

            Instant endReplay = Instant.now();
            long replayDuration = Math.max(1, endReplay.toEpochMilli() - startReplay.toEpochMilli());

            // Build step comparison between original spans and replayed spans
            List<Span> replayedSpans = spanRepository.findByTraceIdOrderByStartTimeAsc(result.getTraceId());
            List<ReplayStepComparison> comparisons = buildStepComparisons(
                    session.getReplayMode(),
                    originalSpans,
                    replayedSpans,
                    faultConfig
            );

            session.setReplayedTraceId(result.getTraceId());
            session.setReplayDurationMs(replayDuration);
            session.setStepsCount(comparisons.size());
            session.setDiffSummary(objectMapper.writeValueAsString(comparisons));
            session.setCompletedAt(endReplay);

            if ("ERROR".equals(result.getStatus()) && !"FAULT_INJECTION".equals(session.getReplayMode())) {
                session.setStatus("FAILED");
                session.setErrorMessage(result.getErrorMessage());
            } else {
                session.setStatus("COMPLETED");
            }

            replaySessionRepository.save(session);
            log.info("Replay session {} completed successfully with mode {}", sessionId, session.getReplayMode());

        } catch (Exception e) {
            log.error("Replay session {} failed: {}", sessionId, e.getMessage(), e);
            session.setStatus("FAILED");
            session.setErrorMessage(e.getMessage());
            session.setCompletedAt(Instant.now());
            replaySessionRepository.save(session);
        }
    }

    private List<ReplayStepComparison> buildStepComparisons(
            String mode,
            List<Span> originalSpans,
            List<Span> replayedSpans,
            FaultConfig faultConfig
    ) {
        List<ReplayStepComparison> list = new ArrayList<>();
        int maxSize = Math.max(originalSpans.size(), replayedSpans.size());

        for (int i = 0; i < maxSize; i++) {
            Span orig = i < originalSpans.size() ? originalSpans.get(i) : null;
            Span rep = i < replayedSpans.size() ? replayedSpans.get(i) : null;

            String opName = rep != null ? rep.getOperationName() : (orig != null ? orig.getOperationName() : "step-" + (i + 1));
            String kind = rep != null ? rep.getSpanKind() : (orig != null ? orig.getSpanKind() : "SPAN");

            String origIn = orig != null ? orig.getInputPayload() : null;
            String origOut = orig != null ? orig.getOutputPayload() : null;
            String repIn = rep != null ? rep.getInputPayload() : null;
            String repOut = rep != null ? rep.getOutputPayload() : null;

            Long origDur = orig != null ? orig.getDurationMs() : 0L;
            Long repDur = rep != null ? rep.getDurationMs() : 0L;

            boolean match = Objects.equals(origOut, repOut) || (orig != null && rep != null && orig.getStatus().equals(rep.getStatus()));
            String stepStatus = "MATCHED";
            String note = "Execution step matched original trace.";

            if ("FAULT_INJECTION".equals(mode) && faultConfig != null &&
                    (faultConfig.getTargetTool() == null || opName.contains(faultConfig.getTargetTool()))) {
                stepStatus = "INJECTED_FAULT";
                note = "Controlled fault injected (" + faultConfig.getFaultType() + "). Tool handled according to resilience policy.";
                match = false;
            } else if (!match) {
                stepStatus = "DIVERGED";
                note = "Payload or duration diverged from recorded baseline.";
            }

            list.add(ReplayStepComparison.builder()
                    .stepOrder(i + 1)
                    .operationName(opName)
                    .eventType(kind)
                    .originalInput(origIn)
                    .originalOutput(origOut)
                    .replayedInput(repIn)
                    .replayedOutput(repOut)
                    .originalDurationMs(origDur)
                    .replayedDurationMs(repDur)
                    .match(match)
                    .status(stepStatus)
                    .diffNote(note)
                    .build());
        }

        return list;
    }

    public Page<ReplaySessionResponse> listReplaySessions(String projectId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return replaySessionRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pr)
                .map(this::toSessionResponse);
    }

    public ReplayDetailResponse getReplayDetail(String projectId, String sessionId) {
        ReplaySession session = replaySessionRepository.findByProjectIdAndId(projectId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Replay session not found: " + sessionId));

        List<ReplayStepComparison> comparisons = Collections.emptyList();
        if (session.getDiffSummary() != null && !session.getDiffSummary().isBlank()) {
            try {
                comparisons = objectMapper.readValue(session.getDiffSummary(), new TypeReference<List<ReplayStepComparison>>() {});
            } catch (Exception ignored) {}
        }

        return ReplayDetailResponse.builder()
                .session(toSessionResponse(session))
                .stepComparisons(comparisons)
                .build();
    }

    @Transactional
    public void cancelReplay(String projectId, String sessionId) {
        ReplaySession session = replaySessionRepository.findByProjectIdAndId(projectId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Replay session not found: " + sessionId));

        if ("PENDING".equals(session.getStatus()) || "RUNNING".equals(session.getStatus())) {
            session.setStatus("CANCELLED");
            session.setCompletedAt(Instant.now());
            replaySessionRepository.save(session);
        }
    }

    private ReplaySessionResponse toSessionResponse(ReplaySession s) {
        return ReplaySessionResponse.builder()
                .id(s.getId())
                .projectId(s.getProject().getId())
                .sourceTraceId(s.getSourceTraceId())
                .replayedTraceId(s.getReplayedTraceId())
                .replayMode(s.getReplayMode())
                .status(s.getStatus())
                .originalDurationMs(s.getOriginalDurationMs())
                .replayDurationMs(s.getReplayDurationMs())
                .stepsCount(s.getStepsCount())
                .diffSummary(s.getDiffSummary())
                .errorMessage(s.getErrorMessage())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }
}
