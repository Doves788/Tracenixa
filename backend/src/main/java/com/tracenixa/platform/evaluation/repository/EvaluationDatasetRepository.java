package com.tracenixa.platform.evaluation.repository;

import com.tracenixa.platform.evaluation.model.EvaluationDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationDatasetRepository extends JpaRepository<EvaluationDataset, String> {
    List<EvaluationDataset> findByProjectId(String projectId);
    Page<EvaluationDataset> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);
    Optional<EvaluationDataset> findByProjectIdAndId(String projectId, String id);
}
