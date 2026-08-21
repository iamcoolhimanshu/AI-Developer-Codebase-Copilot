package com.codecopilot.observability;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-process usage/cost observability. Counters reset on restart;
 * a persistent metrics store can replace this abstraction later.
 */
@Service
public class MetricsService {

    private final AtomicLong aiRequests = new AtomicLong();
    private final AtomicLong aiTokens = new AtomicLong();
    private final AtomicLong toolCalls = new AtomicLong();
    private final AtomicLong retrievals = new AtomicLong();
    private final AtomicLong indexingRuns = new AtomicLong();
    private final AtomicLong failedAiCalls = new AtomicLong();
    private final ConcurrentHashMap<String, AccumulatedLatency> latencies = new ConcurrentHashMap<>();
    private final Instant startedAt = Instant.now();

    public record AccumulatedLatency(long count, long totalMs) {
        public AccumulatedLatency mergeWith(AccumulatedLatency other) {
            return new AccumulatedLatency(count + other.count, totalMs + other.totalMs);
        }
    }

    public void aiRequest(long requestMs) {
        aiRequests.incrementAndGet();
        latencies.merge("ai-request", new AccumulatedLatency(1, requestMs), AccumulatedLatency::mergeWith);
    }

    public void tokens(long count) {
        aiTokens.addAndGet(count);
    }

    public void toolCall() {
        toolCalls.incrementAndGet();
    }

    public void retrieval() {
        retrievals.incrementAndGet();
    }

    public void indexingRun() {
        indexingRuns.incrementAndGet();
    }

    public void failedAiCall() {
        failedAiCalls.incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        return Map.of(
                "startedAt", startedAt.toString(),
                "aiRequests", aiRequests.get(),
                "aiTokens", aiTokens.get(),
                "toolCalls", toolCalls.get(),
                "retrievals", retrievals.get(),
                "indexingRuns", indexingRuns.get(),
                "failedAiCalls", failedAiCalls.get(),
                "latencies", latencies);
    }
}