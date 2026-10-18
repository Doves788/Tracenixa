package com.tracenixa.platform.trace.repository;

import com.tracenixa.platform.trace.model.Span;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpanRepository extends JpaRepository<Span, String> {

    List<Span> findByTraceIdOrderByStartTimeAsc(String traceId);

    Optional<Span> findBySpanId(String spanId);

    List<Span> findByProjectIdAndTraceIdOrderByStartTimeAsc(String projectId, String traceId);

    @Query("SELECT s.operationName, COUNT(s), AVG(s.durationMs), SUM(CASE WHEN s.status = 'ERROR' THEN 1 ELSE 0 END) " +
           "FROM Span s WHERE s.project.id = :projectId AND s.spanKind = 'TOOL' " +
           "GROUP BY s.operationName")
    List<Object[]> getToolCallMetrics(@Param("projectId") String projectId);
}
