package com.tracenixa.platform.project.controller;

import com.tracenixa.platform.project.dto.ProjectDtos.*;
import com.tracenixa.platform.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Workspace and project isolation management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new isolated project workspace")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.createProject(userId, request));
    }

    @GetMapping
    @Operation(summary = "List all workspaces accessible to current user")
    public ResponseEntity<List<ProjectResponse>> listProjects(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(projectService.getUserProjects(userId));
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get workspace details by ID")
    public ResponseEntity<ProjectResponse> getProject(
            @AuthenticationPrincipal String userId,
            @PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId, userId));
    }
}
