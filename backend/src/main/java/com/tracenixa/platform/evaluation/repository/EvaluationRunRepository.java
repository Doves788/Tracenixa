package com.tracenixa.platform.evaluation.repository;

import com.tracenixa.platform.evaluation.model.EvaluationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, String> {
    Page<EvaluationRun> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);
    Optional<EvaluationRun> findByProjectIdAndId(String projectId, String id);
    List<EvaluationRun> findByDatasetIdOrderByCreatedAtDesc(String datasetId);
}
