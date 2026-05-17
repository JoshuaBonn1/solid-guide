package com.solidguide.algorithms.framework;

import java.time.Duration;

/**
 * Aggregated runtime and heap-delta measurements for one algorithm case.
 */
public record ExecutionStats(
        int iterations,
        long totalDurationNanos,
        long minDurationNanos,
        long maxDurationNanos,
        long maxMemoryDeltaBytes,
        long totalMemoryDeltaBytes) {
    public ExecutionStats {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1");
        }
        if (totalDurationNanos < 0 || minDurationNanos < 0 || maxDurationNanos < 0) {
            throw new IllegalArgumentException("duration values must be >= 0");
        }
        if (maxMemoryDeltaBytes < 0 || totalMemoryDeltaBytes < 0) {
            throw new IllegalArgumentException("memory values must be >= 0");
        }
    }

    public long averageDurationNanos() {
        return totalDurationNanos / iterations;
    }

    public long averageMemoryDeltaBytes() {
        return totalMemoryDeltaBytes / iterations;
    }

    public Duration averageDuration() {
        return Duration.ofNanos(averageDurationNanos());
    }

    public Duration minDuration() {
        return Duration.ofNanos(minDurationNanos);
    }

    public Duration maxDuration() {
        return Duration.ofNanos(maxDurationNanos);
    }
}
