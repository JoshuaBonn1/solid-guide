package com.solidguide.algorithms.framework;

public record AgnosticCase(
        String name,
        String inputValue,
        String outputValue,
        Long maxAverageDurationNanos,
        Long maxMemoryDeltaBytes) {
}
