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
zig/       Zig implementation, examples, and self-tests
scripts/   Root entrypoints for testing and benchmark reports
```

## Requirements

- JDK 21 or newer
- Python 3.11 or newer
- Zig 0.14 or newer
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
./scripts/test-zig
```

Print example timing and memory reports for every language:

```bash
./scripts/benchmark
```

Or run one language report:

```bash
./scripts/benchmark-java
./scripts/benchmark-python
./scripts/benchmark-zig
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

## Zig overview

Core declarations live in `zig/src/framework.zig`:

- `Algorithm(Input, Output)`: candidate algorithm function contract.
- `AlgorithmCase(Input, Output)`: reproducible input, expected-output oracle,
  equality strategy, and optional complexity budget.
- `AlgorithmTestSuite(Input, Output)`: runs correctness first, then warmup and
  measured executions.
- `BenchmarkOptions`: warmup count, measurement count, and memory settings.
- `ComplexityBudget`: optional maximum average duration and maximum arena
  allocation delta.
- `AlgorithmSuiteResult` / `AlgorithmCaseResult`: structured results for
  assertions or reporting.
- `AlgorithmTestRunner`: console reporter.

Example suites live in `zig/src/examples.zig` and mirror the Java and Python
examples. The Zig implementation measures memory by running each measured case
inside a fresh `std.heap.ArenaAllocator` and recording the arena capacity used by
that run.

### Add a Zig algorithm

```zig
const std = @import("std");
const framework = @import("framework.zig");
const examples = @import("examples.zig");

const SortCase = framework.AlgorithmCase([]const i32, []i32);

fn mySort(allocator: std.mem.Allocator, input: []const i32) ![]i32 {
    const values = try allocator.alloc(i32, input.len);
    @memcpy(values, input);
    // Sort values here.
    return values;
}

fn duplicates(_: std.mem.Allocator) ![]const i32 {
    return &[_]i32{ 3, 1, 3, 2 };
}

const cases = [_]SortCase{
    .{
        .name = "duplicates",
        .input_factory = duplicates,
        .expected_output = examples.zigSort,
        .equality = struct {
            fn eql(expected: []i32, actual: []i32) bool {
                return std.mem.eql(i32, expected, actual);
            }
        }.eql,
        .budget = framework.ComplexityBudget.withLimits(5_000_000, 1024 * 1024),
    },
};

const suite = framework.AlgorithmTestSuite([]const i32, []i32){
    .suite_name = "sorting",
    .algorithm_name = "my-sort",
    .algorithm = mySort,
    .cases = &cases,
    .options = framework.BenchmarkOptions.quick(),
    .allocator = std.heap.page_allocator,
};
```

Use input suppliers that create fresh data for every run, especially for
algorithms that mutate arrays, lists, or graph structures. Memory measurements
use coarse runtime snapshots (`Runtime` heap checks in Java and `tracemalloc` in
Python, arena capacity in Zig), so they should be treated as regression signals
rather than exact allocation counts.
