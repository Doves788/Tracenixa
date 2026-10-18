package com.tracenixa.platform.project.service;

import com.tracenixa.platform.auth.model.User;
import com.tracenixa.platform.auth.repository.UserRepository;
import com.tracenixa.platform.project.dto.ProjectDtos.*;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(String userId, CreateProjectRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-")
                : request.getName().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6);

        if (projectRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        Project project = Project.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .owner(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        projectRepository.save(project);
        return toResponse(project);
    }

    public List<ProjectResponse> getUserProjects(String userId) {
        return projectRepository.findByOwnerId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Access denied: Project does not belong to user");
        }

        return toResponse(project);
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .slug(project.getSlug())
                .description(project.getDescription())
                .ownerId(project.getOwner().getId())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
