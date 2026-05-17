package com.solidguide.algorithms.framework;

/**
 * Controls how many runs are used to assess speed and memory behavior.
 */
public record BenchmarkOptions(
        int warmupIterations,
        int measurementIterations,
        boolean measureMemory,
        boolean gcBeforeMemoryMeasurement) {
    public BenchmarkOptions {
        if (warmupIterations < 0) {
            throw new IllegalArgumentException("warmupIterations must be >= 0");
        }
        if (measurementIterations < 1) {
            throw new IllegalArgumentException("measurementIterations must be >= 1");
        }
    }

    public static BenchmarkOptions defaults() {
        return new BenchmarkOptions(5, 25, true, true);
    }

    public static BenchmarkOptions quick() {
        return new BenchmarkOptions(1, 5, true, true);
    }

    public BenchmarkOptions withoutMemoryMeasurements() {
        return new BenchmarkOptions(warmupIterations, measurementIterations, false, false);
    }
}
