package com.tracenixa.platform.evaluation.repository;

import com.tracenixa.platform.evaluation.model.RegressionTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegressionTestRepository extends JpaRepository<RegressionTest, String> {
    Page<RegressionTest> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);
    List<RegressionTest> findByProjectIdAndStatus(String projectId, String status);
    Optional<RegressionTest> findByProjectIdAndId(String projectId, String id);
}
