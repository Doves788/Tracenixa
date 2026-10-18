package com.tracenixa.platform.replay.controller;

import com.tracenixa.platform.replay.dto.ReplayDtos.*;
import com.tracenixa.platform.replay.service.ReplayEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/replays")
@RequiredArgsConstructor
@Tag(name = "Record & Replay Engine", description = "Execution reproduction, live reruns, and sandbox fault testing")
public class ReplayController {

    private final ReplayEngineService replayEngineService;

    @PostMapping
    @Operation(summary = "Initiate an asynchronous replay session (RECORDED, LIVE, or FAULT_INJECTION)")
    public ResponseEntity<ReplaySessionResponse> createReplay(
            @PathVariable String projectId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateReplayRequest request) {

        return ResponseEntity.ok(replayEngineService.createReplaySession(projectId, userId, request));
    }

    @GetMapping
    @Operation(summary = "List all replay sessions for the project")
    public ResponseEntity<Page<ReplaySessionResponse>> listReplays(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(replayEngineService.listReplaySessions(projectId, page, size));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get full replay session detail including original vs replayed step diffs")
    public ResponseEntity<ReplayDetailResponse> getReplayDetail(
            @PathVariable String projectId,
            @PathVariable String sessionId) {

        return ResponseEntity.ok(replayEngineService.getReplayDetail(projectId, sessionId));
    }

    @PostMapping("/{sessionId}/cancel")
    @Operation(summary = "Cancel a running or pending replay session")
    public ResponseEntity<Void> cancelReplay(
            @PathVariable String projectId,
            @PathVariable String sessionId) {

        replayEngineService.cancelReplay(projectId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
