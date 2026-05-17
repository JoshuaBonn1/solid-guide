package com.solidguide.algorithms.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runs one algorithm across a set of correctness cases and performance budgets.
 */
public final class AlgorithmTestSuite<I, O> {
    private static volatile int blackhole;

    private final String suiteName;
    private final String algorithmName;
    private final Algorithm<I, O> algorithm;
    private final List<AlgorithmCase<I, O>> cases;
    private final BenchmarkOptions options;

    private AlgorithmTestSuite(
            String suiteName,
            String algorithmName,
            Algorithm<I, O> algorithm,
            List<AlgorithmCase<I, O>> cases,
            BenchmarkOptions options) {
        this.suiteName = AlgorithmCase.requireName(suiteName, "suiteName");
        this.algorithmName = AlgorithmCase.requireName(algorithmName, "algorithmName");
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("at least one case is required");
        }
        this.cases = List.copyOf(cases);
        this.options = Objects.requireNonNull(options, "options");
    }

    public static <I, O> Builder<I, O> builder(String suiteName) {
        return new Builder<>(suiteName);
    }

    public AlgorithmSuiteResult<O> run() {
        List<AlgorithmCaseResult<O>> results = new ArrayList<>();
        for (AlgorithmCase<I, O> testCase : cases) {
            results.add(runCase(testCase));
        }
        return new AlgorithmSuiteResult<>(suiteName, algorithmName, results);
    }

    private AlgorithmCaseResult<O> runCase(AlgorithmCase<I, O> testCase) {
        O expected = null;
        O actual = null;
        try {
            expected = testCase.expectedOutput().apply(testCase.inputFactory().get());
            actual = algorithm.run(testCase.inputFactory().get());
            boolean correct = testCase.equality().test(expected, actual);
            if (!correct) {
                return new AlgorithmCaseResult<>(
                        testCase.name(),
                        false,
                        expected,
                        actual,
                        Optional.empty(),
                        List.of(),
                        Optional.empty());
            }

            ExecutionStats stats = measure(testCase);
            return new AlgorithmCaseResult<>(
                    testCase.name(),
                    true,
                    expected,
                    actual,
                    Optional.of(stats),
                    budgetViolations(testCase.budget(), stats),
                    Optional.empty());
        } catch (Throwable failure) {
            return new AlgorithmCaseResult<>(
                    testCase.name(),
                    false,
                    expected,
                    actual,
                    Optional.empty(),
                    List.of(),
                    Optional.of(failure));
        }
    }

    private ExecutionStats measure(AlgorithmCase<I, O> testCase) throws Exception {
        for (int i = 0; i < options.warmupIterations(); i++) {
            consume(algorithm.run(testCase.inputFactory().get()));
        }

        long totalDuration = 0L;
        long minDuration = Long.MAX_VALUE;
        long maxDuration = 0L;
        long totalMemoryDelta = 0L;
        long maxMemoryDelta = 0L;

        for (int i = 0; i < options.measurementIterations(); i++) {
            if (options.measureMemory() && options.gcBeforeMemoryMeasurement()) {
                MemoryMeter.requestGcPause();
            }

            long memoryBefore = options.measureMemory() ? MemoryMeter.usedHeapBytes() : 0L;
            long started = System.nanoTime();
            O output = algorithm.run(testCase.inputFactory().get());
            long elapsed = System.nanoTime() - started;
            consume(output);
            long memoryAfter = options.measureMemory() ? MemoryMeter.usedHeapBytes() : memoryBefore;
            long memoryDelta = Math.max(0L, memoryAfter - memoryBefore);

            totalDuration += elapsed;
            minDuration = Math.min(minDuration, elapsed);
            maxDuration = Math.max(maxDuration, elapsed);
            totalMemoryDelta += memoryDelta;
            maxMemoryDelta = Math.max(maxMemoryDelta, memoryDelta);
        }

        return new ExecutionStats(
                options.measurementIterations(),
                totalDuration,
                minDuration,
                maxDuration,
                maxMemoryDelta,
                totalMemoryDelta);
    }

    private static List<String> budgetViolations(ComplexityBudget budget, ExecutionStats stats) {
        List<String> violations = new ArrayList<>();
        budget.maxAverageDurationNanos().ifPresent(max -> {
            if (stats.averageDurationNanos() > max) {
                violations.add("average duration " + stats.averageDurationNanos() + "ns exceeded " + max + "ns");
            }
        });
        budget.maxMemoryDeltaBytes().ifPresent(max -> {
            if (stats.maxMemoryDeltaBytes() > max) {
                violations.add("max memory delta " + stats.maxMemoryDeltaBytes() + " bytes exceeded " + max + " bytes");
            }
        });
        return violations;
    }

    private static void consume(Object value) {
        blackhole = 31 * blackhole + Objects.hashCode(value);
    }

    public static final class Builder<I, O> {
        private final String suiteName;
        private String algorithmName;
        private Algorithm<I, O> algorithm;
        private final List<AlgorithmCase<I, O>> cases = new ArrayList<>();
        private BenchmarkOptions options = BenchmarkOptions.defaults();

        private Builder(String suiteName) {
            this.suiteName = suiteName;
        }

        public Builder<I, O> algorithm(String algorithmName, Algorithm<I, O> algorithm) {
            this.algorithmName = algorithmName;
            this.algorithm = algorithm;
            return this;
        }

        public Builder<I, O> addCase(AlgorithmCase<I, O> testCase) {
            this.cases.add(testCase);
            return this;
        }

        public Builder<I, O> options(BenchmarkOptions options) {
            this.options = options;
            return this;
        }

        public AlgorithmTestSuite<I, O> build() {
            return new AlgorithmTestSuite<>(suiteName, algorithmName, algorithm, cases, options);
        }
    }
}
