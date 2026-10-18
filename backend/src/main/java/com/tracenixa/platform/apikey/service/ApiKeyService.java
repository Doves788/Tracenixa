package com.tracenixa.platform.apikey.service;

import com.tracenixa.platform.apikey.dto.ApiKeyDtos.*;
import com.tracenixa.platform.apikey.model.ApiKey;
import com.tracenixa.platform.apikey.repository.ApiKeyRepository;
import com.tracenixa.platform.apikey.util.ApiKeyHasher;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public ApiKeyCreatedResponse createApiKey(String projectId, String userId, CreateApiKeyRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Access denied: Not authorized for project " + projectId);
        }

        String plaintext = ApiKeyHasher.generatePlaintextKey();
        String hash = ApiKeyHasher.hashKey(plaintext);
        String prefix = ApiKeyHasher.extractPrefix(plaintext);

        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID().toString())
                .project(project)
                .name(request.getName().trim())
                .keyPrefix(prefix)
                .keyHash(hash)
                .createdBy(userId)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        apiKeyRepository.save(apiKey);

        return ApiKeyCreatedResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(prefix)
                .plaintextKey(plaintext)
                .status(apiKey.getStatus())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    public List<ApiKeyResponse> getProjectApiKeys(String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Access denied: Not authorized for project " + projectId);
        }

        return apiKeyRepository.findByProjectId(projectId).stream()
                .map(k -> ApiKeyResponse.builder()
                        .id(k.getId())
                        .name(k.getName())
                        .keyPrefix(k.getKeyPrefix())
                        .status(k.getStatus())
                        .lastUsedAt(k.getLastUsedAt())
                        .createdAt(k.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeApiKey(String projectId, String keyId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (!project.getOwner().getId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));

        if (!apiKey.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("API key does not belong to project");
        }

        apiKey.setStatus("REVOKED");
        apiKeyRepository.save(apiKey);
    }

    @Transactional
    public Optional<ApiKey> validateAndTouchApiKey(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            return Optional.empty();
        }
        String hash = ApiKeyHasher.hashKey(plaintextKey.trim());
        Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyHashAndStatus(hash, "ACTIVE");
        keyOpt.ifPresent(key -> {
            key.setLastUsedAt(Instant.now());
            apiKeyRepository.save(key);
        });
        return keyOpt;
    }
}
