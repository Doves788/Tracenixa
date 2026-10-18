package com.tracenixa.platform.apikey.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;

public class ApiKeyDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateApiKeyRequest {
        @NotBlank(message = "API key name is required")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiKeyCreatedResponse {
        private String id;
        private String name;
        private String keyPrefix;
        private String plaintextKey; // Returned ONLY ONCE upon creation
        private String status;
        private Instant createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiKeyResponse {
        private String id;
        private String name;
        private String keyPrefix;
        private String status;
        private Instant lastUsedAt;
        private Instant createdAt;
    }
}
