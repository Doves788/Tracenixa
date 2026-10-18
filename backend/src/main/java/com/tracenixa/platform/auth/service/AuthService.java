package com.tracenixa.platform.auth.service;

import com.tracenixa.platform.apikey.model.ApiKey;
import com.tracenixa.platform.apikey.repository.ApiKeyRepository;
import com.tracenixa.platform.apikey.util.ApiKeyHasher;
import com.tracenixa.platform.auth.dto.AuthDtos.*;
import com.tracenixa.platform.auth.model.User;
import com.tracenixa.platform.auth.repository.UserRepository;
import com.tracenixa.platform.auth.security.JwtUtils;
import com.tracenixa.platform.project.model.Project;
import com.tracenixa.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String userId = UUID.randomUUID().toString();
        User user = User.builder()
                .id(userId)
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role("DEVELOPER")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(user);

        // Auto-provision initial default project
        String projectId = UUID.randomUUID().toString();
        Project defaultProject = Project.builder()
                .id(projectId)
                .name("Default Project")
                .slug("default-project-" + userId.substring(0, 8))
                .description("Default workspace for AI agent observability and replay")
                .owner(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        projectRepository.save(defaultProject);

        // Auto-provision initial live API key
        String plaintextKey = ApiKeyHasher.generatePlaintextKey();
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID().toString())
                .project(defaultProject)
                .name("Default API Key")
                .keyPrefix(ApiKeyHasher.extractPrefix(plaintextKey))
                .keyHash(ApiKeyHasher.hashKey(plaintextKey))
                .createdBy(userId)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
        apiKeyRepository.save(apiKey);

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
