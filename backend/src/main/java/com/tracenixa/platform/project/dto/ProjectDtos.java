package com.tracenixa.platform.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

public class ProjectDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateProjectRequest {
        @NotBlank(message = "Project name is required")
        private String name;

        private String slug;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectResponse {
        private String id;
        private String name;
        private String slug;
        private String description;
        private String ownerId;
        private Instant createdAt;
    }
}
