package com.tracenixa.platform.replay.repository;

import com.tracenixa.platform.replay.model.ReplaySession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplaySessionRepository extends JpaRepository<ReplaySession, String> {
    Page<ReplaySession> findByProjectIdOrderByCreatedAtDesc(String projectId, Pageable pageable);
    Optional<ReplaySession> findByProjectIdAndId(String projectId, String id);
    List<ReplaySession> findBySourceTraceId(String sourceTraceId);
}
