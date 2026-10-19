package com.tracenixa.platform.trace.repository;

import com.tracenixa.platform.trace.model.Trace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TraceRepository extends JpaRepository<Trace, String> {

    Optional<Trace> findByTraceId(String traceId);

    Optional<Trace> findByProjectIdAndTraceId(String projectId, String traceId);

    Page<Trace> findByProjectIdOrderByStartTimeDesc(String projectId, Pageable pageable);

    @Query("SELECT t FROM Trace t WHERE t.project.id = :projectId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:serviceName IS NULL OR t.serviceName = :serviceName) " +
           "AND (:search IS NULL OR LOWER(t.traceId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.rootOperationName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.startTime DESC")
    Page<Trace> searchTraces(
        @Param("projectId") String projectId,
        @Param("status") String status,
        @Param("serviceName") String serviceName,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("SELECT COUNT(t) FROM Trace t WHERE t.project.id = :projectId")
    long countByProjectId(@Param("projectId") String projectId);

    @Query("SELECT COUNT(t) FROM Trace t WHERE t.project.id = :projectId AND t.status = 'ERROR'")
    long countErrorsByProjectId(@Param("projectId") String projectId);

    @Query("SELECT AVG(t.durationMs) FROM Trace t WHERE t.project.id = :projectId")
    Double getAverageDurationByProjectId(@Param("projectId") String projectId);

    @Query("SELECT SUM(t.totalTokens) FROM Trace t WHERE t.project.id = :projectId")
    Long getTotalTokensByProjectId(@Param("projectId") String projectId);

    @Query("SELECT SUM(t.estimatedCost) FROM Trace t WHERE t.project.id = :projectId")
    Double getTotalCostByProjectId(@Param("projectId") String projectId);

    @Query("SELECT t.modelName, SUM(t.totalTokens), COUNT(t) FROM Trace t WHERE t.project.id = :projectId GROUP BY t.modelName")
    List<Object[]> getTokensByModel(@Param("projectId") String projectId);

    List<Trace> findTop10ByProjectIdAndStatusOrderByStartTimeDesc(String projectId, String status);

    List<Trace> findByProjectIdAndStartTimeBetweenOrderByStartTimeAsc(String projectId, Instant start, Instant end);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Trace t WHERE t.startTime < :cutoff")
    int deleteTracesOlderThan(@Param("cutoff") Instant cutoff);
}
