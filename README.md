# solid-guide

Algorithm exploration in Java.

This repository contains a small, dependency-free framework for testing
algorithms against three concerns:

- **Correctness**: compare each candidate algorithm with a reference oracle.
- **Speed**: collect min, max, and average execution time over repeat runs.
- **Memory usage**: collect approximate heap deltas for each measured run.

The framework is intentionally lightweight so it can be used for exercises,
experiments, and regression checks without requiring a benchmarking dependency.
For production-grade microbenchmarks, use JMH; this framework is designed to
catch obvious correctness failures and relative performance regressions while
keeping algorithm examples easy to read.

## Requirements

- JDK 21 or newer
- Optional: Maven 3.9+ if you prefer `mvn test`

## Run tests

The included scripts compile with `javac` and do not require Maven:

```bash
./scripts/test
```

To print example timing and memory reports:

```bash
./scripts/benchmark
```

If Maven is installed, the same self-tests can be run with:

```bash
mvn test
```

## Framework overview

Core classes live in `src/main/java/com/solidguide/algorithms/framework`:

- `Algorithm<I, O>`: candidate algorithm contract.
- `AlgorithmCase<I, O>`: reproducible input, expected-output oracle, equality
  strategy, and optional complexity budget.
- `AlgorithmTestSuite<I, O>`: runs correctness first, then warmup and measured
  executions.
- `BenchmarkOptions`: warmup count, measurement count, and memory settings.
- `ComplexityBudget`: optional maximum average duration and maximum heap delta.
- `AlgorithmSuiteResult` / `AlgorithmCaseResult`: structured results for
  assertions or reporting.
- `AlgorithmTestRunner`: console reporter and process-exit helper.

Example suites live in `src/main/java/com/solidguide/algorithms/examples` and
cover insertion sort, merge sort, and binary search.

## Add a new algorithm

```java
AlgorithmTestSuite<List<Integer>, List<Integer>> suite =
        AlgorithmTestSuite.<List<Integer>, List<Integer>>builder("sorting")
                .algorithm("my-sort", MyAlgorithms::sort)
                .options(BenchmarkOptions.quick())
                .addCase(AlgorithmCase.<List<Integer>, List<Integer>>builder("duplicates")
                        .input(() -> List.of(3, 1, 3, 2))
                        .expect(SortingAlgorithms::javaSort)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofMillis(5))
                                .maxMemoryDeltaBytes(1024 * 1024)
                                .build())
                        .build())
                .build();

AlgorithmTestRunner.runAndExit(suite.run());
```

Use input suppliers that create fresh data for every run, especially for
algorithms that mutate arrays, lists, or graph structures. Memory measurements
use `Runtime` heap snapshots around each execution, so they should be treated as
coarse regression signals rather than exact allocation counts.
