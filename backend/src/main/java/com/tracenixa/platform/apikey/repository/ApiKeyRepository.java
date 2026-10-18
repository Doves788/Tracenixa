package com.tracenixa.platform.apikey.repository;

import com.tracenixa.platform.apikey.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    List<ApiKey> findByProjectId(String projectId);
    Optional<ApiKey> findByKeyHash(String keyHash);
    Optional<ApiKey> findByKeyHashAndStatus(String keyHash, String status);
}
