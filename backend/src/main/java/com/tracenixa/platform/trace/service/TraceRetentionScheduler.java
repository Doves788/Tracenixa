package com.tracenixa.platform.trace.service;

import com.tracenixa.platform.trace.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Enterprise Time-Series Partition Maintenance & Data Retention Worker.
 * Automatically purges stale trace telemetry according to retention policies (e.g., 30-day TTL),
 * preventing unbounded database growth and maintaining high-performance B-tree index density.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceRetentionScheduler {

    private final TraceRepository traceRepository;

    @Value("${tracenixa.retention.days:30}")
    private int retentionDays;

    @Transactional
    @Scheduled(cron = "${tracenixa.retention.cron:0 0 2 * * ?}") // 2 AM daily maintenance window
    public int enforceDataRetentionTtl() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Running automated trace TTL retention cleanup. Purging traces prior to: {}", cutoff);
        int deleted = traceRepository.deleteTracesOlderThan(cutoff);
        log.info("Trace TTL retention cleanup completed. Purged {} expired trace records.", deleted);
        return deleted;
    }
}

