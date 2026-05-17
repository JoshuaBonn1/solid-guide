package com.solidguide.algorithms.framework;

import java.time.Duration;
import java.util.OptionalLong;

/**
 * Optional limits used to catch speed or memory regressions.
 */
public final class ComplexityBudget {
    private final OptionalLong maxAverageDurationNanos;
    private final OptionalLong maxMemoryDeltaBytes;

    private ComplexityBudget(OptionalLong maxAverageDurationNanos, OptionalLong maxMemoryDeltaBytes) {
        this.maxAverageDurationNanos = maxAverageDurationNanos;
        this.maxMemoryDeltaBytes = maxMemoryDeltaBytes;
    }

    public static ComplexityBudget none() {
        return new ComplexityBudget(OptionalLong.empty(), OptionalLong.empty());
    }

    public static Builder builder() {
        return new Builder();
    }

    public OptionalLong maxAverageDurationNanos() {
        return maxAverageDurationNanos;
    }

    public OptionalLong maxMemoryDeltaBytes() {
        return maxMemoryDeltaBytes;
    }

    public boolean isConstrained() {
        return maxAverageDurationNanos.isPresent() || maxMemoryDeltaBytes.isPresent();
    }

    public static final class Builder {
        private OptionalLong maxAverageDurationNanos = OptionalLong.empty();
        private OptionalLong maxMemoryDeltaBytes = OptionalLong.empty();

        public Builder maxAverageDuration(Duration duration) {
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("duration must be positive");
            }
            maxAverageDurationNanos = OptionalLong.of(duration.toNanos());
            return this;
        }

        public Builder maxMemoryDeltaBytes(long bytes) {
            if (bytes < 0) {
                throw new IllegalArgumentException("bytes must be >= 0");
            }
            maxMemoryDeltaBytes = OptionalLong.of(bytes);
            return this;
        }

        public ComplexityBudget build() {
            return new ComplexityBudget(maxAverageDurationNanos, maxMemoryDeltaBytes);
        }
    }
}
