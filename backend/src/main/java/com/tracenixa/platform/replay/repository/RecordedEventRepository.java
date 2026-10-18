package com.tracenixa.platform.replay.repository;

import com.tracenixa.platform.replay.model.RecordedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordedEventRepository extends JpaRepository<RecordedEvent, String> {
    List<RecordedEvent> findByTraceIdOrderByStepOrderAsc(String traceId);
    void deleteByTraceId(String traceId);
}
