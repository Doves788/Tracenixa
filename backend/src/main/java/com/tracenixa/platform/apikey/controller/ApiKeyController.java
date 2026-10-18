package com.tracenixa.platform.apikey.controller;

import com.tracenixa.platform.apikey.dto.ApiKeyDtos.*;
import com.tracenixa.platform.apikey.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Project-scoped API key management")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Generate new API key for the project (plaintext revealed once)")
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @PathVariable String projectId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.ok(apiKeyService.createApiKey(projectId, userId, request));
    }

    @GetMapping
    @Operation(summary = "List all active and revoked API keys for the project")
    public ResponseEntity<List<ApiKeyResponse>> listApiKeys(
            @PathVariable String projectId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(apiKeyService.getProjectApiKeys(projectId, userId));
    }

    @DeleteMapping("/{keyId}")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable String projectId,
            @PathVariable String keyId,
            @AuthenticationPrincipal String userId) {
        apiKeyService.revokeApiKey(projectId, keyId, userId);
        return ResponseEntity.noContent().build();
    }
}
