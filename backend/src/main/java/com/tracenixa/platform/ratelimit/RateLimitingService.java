package com.tracenixa.platform.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Creates an enterprise Token Bucket:
     * - Capacity: 200 tokens (handles bursty trace batches)
     * - Refill: 100 tokens per second (greedy continuous refill)
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(200, Refill.greedy(100, Duration.ofSeconds(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String clientIdentifier) {
        Bucket bucket = buckets.computeIfAbsent(clientIdentifier, k -> createNewBucket());
        return bucket.tryConsume(1);
    }

    public long getAvailableTokens(String clientIdentifier) {
        Bucket bucket = buckets.computeIfAbsent(clientIdentifier, k -> createNewBucket());
        return bucket.getAvailableTokens();
    }
}

