package com.tracenixa.platform.auth.security;

import com.tracenixa.platform.apikey.model.ApiKey;
import com.tracenixa.platform.apikey.service.ApiKeyService;
import com.tracenixa.platform.project.context.ProjectContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String apiKeyHeader = request.getHeader("X-API-Key");
            if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer trx_live_")) {
                    apiKeyHeader = authHeader.substring(7);
                }
            }

            if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
                Optional<ApiKey> apiKeyOpt = apiKeyService.validateAndTouchApiKey(apiKeyHeader);
                if (apiKeyOpt.isPresent()) {
                    ApiKey apiKey = apiKeyOpt.get();
                    ProjectContext.setProjectId(apiKey.getProject().getId());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            apiKey.getCreatedBy() != null ? apiKey.getCreatedBy() : "api-key-user",
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            ProjectContext.clear();
        }
    }
}
