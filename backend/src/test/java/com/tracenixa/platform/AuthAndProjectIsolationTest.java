package com.tracenixa.platform;

import com.tracenixa.platform.apikey.dto.ApiKeyDtos.CreateApiKeyRequest;
import com.tracenixa.platform.apikey.service.ApiKeyService;
import com.tracenixa.platform.auth.dto.AuthDtos.*;
import com.tracenixa.platform.auth.service.AuthService;
import com.tracenixa.platform.project.dto.ProjectDtos.CreateProjectRequest;
import com.tracenixa.platform.project.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AuthAndProjectIsolationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Test
    @DisplayName("User registration generates JWT, auto-provisions default workspace and API key")
    void testRegistrationAndAutoProvisioning() {
        RegisterRequest req = RegisterRequest.builder()
                .email("test.engineer@tracenixa.io")
                .password("Secr3tPassword!")
                .fullName("Test Engineer")
                .build();

        AuthResponse authRes = authService.register(req);
        assertThat(authRes.getToken()).isNotBlank();
        assertThat(authRes.getUserId()).isNotBlank();
        assertThat(authRes.getEmail()).isEqualTo("test.engineer@tracenixa.io");

        var projects = projectService.getUserProjects(authRes.getUserId());
        assertThat(projects).isNotEmpty();
        assertThat(projects.get(0).getName()).isEqualTo("Default Project");

        var keys = apiKeyService.getProjectApiKeys(projects.get(0).getId(), authRes.getUserId());
        assertThat(keys).isNotEmpty();
        assertThat(keys.get(0).getKeyPrefix()).startsWith("trx_live_");
    }

    @Test
    @DisplayName("Project isolation prevents cross-project unauthorized access")
    void testProjectIsolation() {
        AuthResponse userA = authService.register(RegisterRequest.builder()
                .email("user.a@tracenixa.io")
                .password("Password123!")
                .fullName("User A")
                .build());

        AuthResponse userB = authService.register(RegisterRequest.builder()
                .email("user.b@tracenixa.io")
                .password("Password123!")
                .fullName("User B")
                .build());

        var projectA = projectService.getUserProjects(userA.getUserId()).get(0);

        // User B attempts to access User A's project
        assertThatThrownBy(() -> projectService.getProjectById(projectA.getId(), userB.getUserId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");

        // User B attempts to generate API key for User A's project
        assertThatThrownBy(() -> apiKeyService.createApiKey(
                projectA.getId(),
                userB.getUserId(),
                CreateApiKeyRequest.builder().name("Malicious Key").build()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }
}
