package com.tracenixa.platform.analytics.repository;

import com.tracenixa.platform.analytics.model.UsageMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageMetricRepository extends JpaRepository<UsageMetric, String> {
    Optional<UsageMetric> findByProjectIdAndMetricDate(String projectId, LocalDate metricDate);
    List<UsageMetric> findByProjectIdAndMetricDateBetweenOrderByMetricDateAsc(String projectId, LocalDate start, LocalDate end);
}
