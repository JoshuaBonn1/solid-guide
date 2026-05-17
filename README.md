# solid-guide

Algorithm exploration across multiple languages.

This repository contains small, dependency-free frameworks for testing
algorithms against three concerns in each supported language:

- **Correctness**: compare each candidate algorithm with a reference oracle.
- **Speed**: collect min, max, and average execution time over repeat runs.
- **Memory usage**: collect approximate memory deltas for each measured run.

The implementations are intentionally lightweight so they can be used for
exercises, experiments, and regression checks without requiring benchmarking
dependencies. For production-grade microbenchmarks, use specialized tools such
as JMH for Java or pyperf for Python; these frameworks are designed to catch
obvious correctness failures and relative performance regressions while keeping
algorithm examples easy to read.

## Layout

```text
java/      Java implementation, examples, and self-tests
python/    Python implementation, examples, and self-tests
scripts/   Root entrypoints for testing and benchmark reports
```

## Requirements

- JDK 21 or newer
- Python 3.11 or newer
- Optional: Maven 3.9+ if you prefer `mvn test`

## Run tests

Run every language implementation:

```bash
./scripts/test
```

Run one language:

```bash
./scripts/test-java
./scripts/test-python
```

Print example timing and memory reports for every language:

```bash
./scripts/benchmark
```

Or run one language report:

```bash
./scripts/benchmark-java
./scripts/benchmark-python
```

If Maven is installed, Java self-tests can also be run with:

```bash
cd java
mvn test
```

## Java overview

Core classes live in `java/src/main/java/com/solidguide/algorithms/framework`:

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

Example suites live in `java/src/main/java/com/solidguide/algorithms/examples`
and cover insertion sort, merge sort, and binary search.

### Add a Java algorithm

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

## Python overview

Core classes live in `python/solid_guide_algorithms/framework.py`:

- `AlgorithmCase[I, O]`: reproducible input, expected-output oracle, equality
  strategy, and optional complexity budget.
- `AlgorithmTestSuite[I, O]`: runs correctness first, then warmup and measured
  executions.
- `BenchmarkOptions`: warmup count, measurement count, and memory settings.
- `ComplexityBudget`: optional maximum average duration and maximum memory
  delta.
- `AlgorithmSuiteResult` / `AlgorithmCaseResult`: structured results for
  assertions or reporting.
- `AlgorithmTestRunner`: console reporter and process-exit helper.

Example suites live in `python/solid_guide_algorithms/examples.py` and mirror
the Java examples.

### Add a Python algorithm

```python
from solid_guide_algorithms import (
    AlgorithmCase,
    AlgorithmTestRunner,
    AlgorithmTestSuite,
    BenchmarkOptions,
    ComplexityBudget,
)
from solid_guide_algorithms.examples import python_sort

suite = AlgorithmTestSuite(
    suite_name="sorting",
    algorithm_name="my-sort",
    algorithm=my_sort,
    options=BenchmarkOptions.quick(),
    cases=(
        AlgorithmCase(
            name="duplicates",
            input_factory=lambda: [3, 1, 3, 2],
            expected_output=python_sort,
            budget=ComplexityBudget.with_limits(
                max_average_duration_seconds=0.005,
                max_memory_delta_bytes=1024 * 1024,
            ),
        ),
    ),
)

AlgorithmTestRunner.run_and_exit(suite.run())
```

Use input suppliers that create fresh data for every run, especially for
algorithms that mutate arrays, lists, or graph structures. Memory measurements
use coarse runtime snapshots (`Runtime` heap checks in Java and `tracemalloc` in
Python), so they should be treated as regression signals rather than exact
allocation counts.
