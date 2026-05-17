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
cases/     Language-agnostic input/output case definitions
scripts/   Root entrypoints for testing and benchmark reports
```

## Requirements

- JDK 21 or newer
- Python 3.11 or newer
- Zig 0.16 or newer
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

## Language-agnostic inputs and outputs

Shared algorithm cases live under `cases/`. Each case file is tab-separated and
stores neutral input/output values plus portable budgets:

```text
name	input	output	max_average_duration_ns	max_memory_delta_bytes
duplicates	[3,1,3,2]	[1,2,3,3]	5000000	1048576
```

The value cells use a JSON-like grammar for common algorithm data structures:
scalars, strings, booleans, lists, matrices, records, and graph-style records
such as `{"nodes":4,"edges":[[0,1],[1,2]]}`. Each language owns small adapter
code that converts those neutral values into local types, for example
`List<Integer>`, `list[int]`, or Zig slices.

The adapter boundary is also where in-place algorithms are supported. A shared
case only describes the value before and after the algorithm. Per-language
adapters provide fresh mutable inputs for in-place algorithms, run the mutation,
and return the mutated structure as the comparable output. Out-of-place
algorithms receive the same neutral values through immutable or copy-on-write
adapters.

Current shared cases cover:

- `cases/sorting.tsv`: list inputs/outputs usable by both out-of-place and
  in-place sorting algorithms.
- `cases/search.tsv`: record input (`values`, `target`) with scalar output.
- `cases/two_sum.tsv` (easy): hash-map lookup over integer lists.
- `cases/valid_parentheses.tsv` (easy): stack validation over strings.
- `cases/number_of_islands.tsv` (medium): DFS grid traversal over matrices.
- `cases/course_schedule.tsv` (medium): graph cycle detection / topological
  ordering over prerequisite edges.
- `cases/trapping_rain_water.tsv` (hard): two-pointer scan over elevation lists.
- `cases/best_time_stock.tsv` (easy): one-pass minimum tracking over prices.
- `cases/valid_anagram.tsv` (easy): character frequency counting over strings.
- `cases/maximum_subarray.tsv` (medium): Kadane dynamic programming over lists.
- `cases/merge_intervals.tsv` (medium): interval sorting and merging over
  matrices.
- `cases/edit_distance.tsv` (hard): dynamic programming over two strings.

Add new structures by extending the shared value shape and implementing a small
adapter in each language. Keep algorithm-specific construction in adapter code
rather than duplicating cases per language.

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
- `AgnosticCases`: shared case loader plus adapters for neutral values and
  in-place list algorithms.

Example suites live in `java/src/main/java/com/solidguide/algorithms/examples`
and cover insertion sort, merge sort, binary search, plus the shared coding
problem suites.

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
- `solid_guide_algorithms.agnostic`: shared case loader plus adapters for
  neutral values and in-place list algorithms.

Example suites live in `python/solid_guide_algorithms/examples.py` and
`python/solid_guide_algorithms/problems.py`; they mirror the Java examples and
the shared coding problem suites.

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
- `agnostic.zig`: shared case parser plus adapters for neutral values and
  in-place / out-of-place case construction.

Example suites live in `zig/src/examples.zig` and `zig/src/problems.zig`; they
mirror the Java and Python examples plus the shared coding problem suites.
The Zig implementation measures memory by running each measured case inside a
fresh `std.heap.ArenaAllocator` and recording the arena capacity used by that
run.

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
algorithms that mutate arrays, lists, or graph structures. Benchmark timing
starts after the input supplier/adapters finish and stops as soon as the
candidate algorithm returns, so input adaptation and output comparison are not
included in speed measurements. Memory measurements use the same algorithm-only
window with coarse runtime snapshots (`Runtime` heap checks in Java and
`tracemalloc` in Python, arena capacity in Zig), so they should be treated as
regression signals rather than exact allocation counts.
