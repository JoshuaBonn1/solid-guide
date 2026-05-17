from __future__ import annotations

from collections.abc import Callable, Sequence
from dataclasses import dataclass, field
from typing import Generic, TypeVar
import gc
import sys
import time
import tracemalloc

I = TypeVar("I")
O = TypeVar("O")

Algorithm = Callable[[I], O]


@dataclass(frozen=True)
class BenchmarkOptions:
    """Controls how many runs are used to assess speed and memory behavior."""

    warmup_iterations: int = 5
    measurement_iterations: int = 25
    measure_memory: bool = True
    gc_before_memory_measurement: bool = True

    def __post_init__(self) -> None:
        if self.warmup_iterations < 0:
            raise ValueError("warmup_iterations must be >= 0")
        if self.measurement_iterations < 1:
            raise ValueError("measurement_iterations must be >= 1")

    @classmethod
    def defaults(cls) -> BenchmarkOptions:
        return cls()

    @classmethod
    def quick(cls) -> BenchmarkOptions:
        return cls(warmup_iterations=1, measurement_iterations=5)

    def without_memory_measurements(self) -> BenchmarkOptions:
        return BenchmarkOptions(
            warmup_iterations=self.warmup_iterations,
            measurement_iterations=self.measurement_iterations,
            measure_memory=False,
            gc_before_memory_measurement=False,
        )


@dataclass(frozen=True)
class ComplexityBudget:
    """Optional limits used to catch speed or memory regressions."""

    max_average_duration_ns: int | None = None
    max_memory_delta_bytes: int | None = None

    def __post_init__(self) -> None:
        if self.max_average_duration_ns is not None and self.max_average_duration_ns <= 0:
            raise ValueError("max_average_duration_ns must be positive")
        if self.max_memory_delta_bytes is not None and self.max_memory_delta_bytes < 0:
            raise ValueError("max_memory_delta_bytes must be >= 0")

    @classmethod
    def none(cls) -> ComplexityBudget:
        return cls()

    @classmethod
    def with_limits(
        cls,
        *,
        max_average_duration_seconds: float | None = None,
        max_memory_delta_bytes: int | None = None,
    ) -> ComplexityBudget:
        duration_ns = None
        if max_average_duration_seconds is not None:
            duration_ns = int(max_average_duration_seconds * 1_000_000_000)
        return cls(
            max_average_duration_ns=duration_ns,
            max_memory_delta_bytes=max_memory_delta_bytes,
        )

    @property
    def is_constrained(self) -> bool:
        return self.max_average_duration_ns is not None or self.max_memory_delta_bytes is not None


@dataclass(frozen=True)
class ExecutionStats:
    """Aggregated runtime and allocation measurements for one algorithm case."""

    iterations: int
    total_duration_ns: int
    min_duration_ns: int
    max_duration_ns: int
    max_memory_delta_bytes: int
    total_memory_delta_bytes: int

    def __post_init__(self) -> None:
        if self.iterations < 1:
            raise ValueError("iterations must be >= 1")
        if min(self.total_duration_ns, self.min_duration_ns, self.max_duration_ns) < 0:
            raise ValueError("duration values must be >= 0")
        if min(self.max_memory_delta_bytes, self.total_memory_delta_bytes) < 0:
            raise ValueError("memory values must be >= 0")

    @property
    def average_duration_ns(self) -> int:
        return self.total_duration_ns // self.iterations

    @property
    def average_memory_delta_bytes(self) -> int:
        return self.total_memory_delta_bytes // self.iterations


@dataclass(frozen=True)
class AlgorithmCase(Generic[I, O]):
    """A reproducible input and oracle for one algorithm scenario."""

    name: str
    input_factory: Callable[[], I]
    expected_output: Callable[[I], O]
    equality: Callable[[O, O], bool] = lambda expected, actual: expected == actual
    budget: ComplexityBudget = field(default_factory=ComplexityBudget.none)

    def __post_init__(self) -> None:
        if not self.name or self.name.isspace():
            raise ValueError("name must not be blank")


@dataclass(frozen=True)
class AlgorithmCaseResult(Generic[O]):
    """Result for one case after correctness, speed, and memory checks."""

    case_name: str
    correct: bool
    expected: O | None
    actual: O | None
    stats: ExecutionStats | None = None
    budget_violations: tuple[str, ...] = ()
    error: BaseException | None = None

    @property
    def passed(self) -> bool:
        return self.correct and not self.budget_violations and self.error is None


@dataclass(frozen=True)
class AlgorithmSuiteResult(Generic[O]):
    """Result for an algorithm across all registered cases."""

    suite_name: str
    algorithm_name: str
    case_results: tuple[AlgorithmCaseResult[O], ...]

    @property
    def passed(self) -> bool:
        return all(result.passed for result in self.case_results)

    @property
    def passed_case_count(self) -> int:
        return sum(1 for result in self.case_results if result.passed)

    @property
    def failed_case_count(self) -> int:
        return len(self.case_results) - self.passed_case_count


class AlgorithmTestSuite(Generic[I, O]):
    """Runs one algorithm across correctness cases and performance budgets."""

    _blackhole = 0

    def __init__(
        self,
        *,
        suite_name: str,
        algorithm_name: str,
        algorithm: Algorithm[I, O],
        cases: Sequence[AlgorithmCase[I, O]],
        options: BenchmarkOptions | None = None,
    ) -> None:
        if not suite_name or suite_name.isspace():
            raise ValueError("suite_name must not be blank")
        if not algorithm_name or algorithm_name.isspace():
            raise ValueError("algorithm_name must not be blank")
        if not cases:
            raise ValueError("at least one case is required")

        self.suite_name = suite_name
        self.algorithm_name = algorithm_name
        self.algorithm = algorithm
        self.cases = tuple(cases)
        self.options = options or BenchmarkOptions.defaults()

    def run(self) -> AlgorithmSuiteResult[O]:
        return AlgorithmSuiteResult(
            suite_name=self.suite_name,
            algorithm_name=self.algorithm_name,
            case_results=tuple(self._run_case(test_case) for test_case in self.cases),
        )

    def _run_case(self, test_case: AlgorithmCase[I, O]) -> AlgorithmCaseResult[O]:
        expected: O | None = None
        actual: O | None = None
        try:
            expected = test_case.expected_output(test_case.input_factory())
            actual = self.algorithm(test_case.input_factory())
            correct = test_case.equality(expected, actual)
            if not correct:
                return AlgorithmCaseResult(
                    case_name=test_case.name,
                    correct=False,
                    expected=expected,
                    actual=actual,
                )

            stats = self._measure(test_case)
            return AlgorithmCaseResult(
                case_name=test_case.name,
                correct=True,
                expected=expected,
                actual=actual,
                stats=stats,
                budget_violations=tuple(self._budget_violations(test_case.budget, stats)),
            )
        except BaseException as failure:
            return AlgorithmCaseResult(
                case_name=test_case.name,
                correct=False,
                expected=expected,
                actual=actual,
                error=failure,
            )

    def _measure(self, test_case: AlgorithmCase[I, O]) -> ExecutionStats:
        for _ in range(self.options.warmup_iterations):
            self._consume(self.algorithm(test_case.input_factory()))

        total_duration_ns = 0
        min_duration_ns = sys.maxsize
        max_duration_ns = 0
        total_memory_delta_bytes = 0
        max_memory_delta_bytes = 0

        for _ in range(self.options.measurement_iterations):
            if self.options.measure_memory and self.options.gc_before_memory_measurement:
                gc.collect()

            memory_delta = 0
            if self.options.measure_memory:
                tracemalloc.start()
                tracemalloc.reset_peak()
                memory_before, _ = tracemalloc.get_traced_memory()
            else:
                memory_before = 0

            started_ns = time.perf_counter_ns()
            output = self.algorithm(test_case.input_factory())
            elapsed_ns = time.perf_counter_ns() - started_ns
            self._consume(output)

            if self.options.measure_memory:
                _, peak_memory = tracemalloc.get_traced_memory()
                tracemalloc.stop()
                memory_delta = max(0, peak_memory - memory_before)

            total_duration_ns += elapsed_ns
            min_duration_ns = min(min_duration_ns, elapsed_ns)
            max_duration_ns = max(max_duration_ns, elapsed_ns)
            total_memory_delta_bytes += memory_delta
            max_memory_delta_bytes = max(max_memory_delta_bytes, memory_delta)

        return ExecutionStats(
            iterations=self.options.measurement_iterations,
            total_duration_ns=total_duration_ns,
            min_duration_ns=min_duration_ns,
            max_duration_ns=max_duration_ns,
            max_memory_delta_bytes=max_memory_delta_bytes,
            total_memory_delta_bytes=total_memory_delta_bytes,
        )

    @staticmethod
    def _budget_violations(budget: ComplexityBudget, stats: ExecutionStats) -> list[str]:
        violations: list[str] = []
        if (
            budget.max_average_duration_ns is not None
            and stats.average_duration_ns > budget.max_average_duration_ns
        ):
            violations.append(
                "average duration "
                f"{stats.average_duration_ns}ns exceeded {budget.max_average_duration_ns}ns"
            )
        if (
            budget.max_memory_delta_bytes is not None
            and stats.max_memory_delta_bytes > budget.max_memory_delta_bytes
        ):
            violations.append(
                "max memory delta "
                f"{stats.max_memory_delta_bytes} bytes exceeded {budget.max_memory_delta_bytes} bytes"
            )
        return violations

    @classmethod
    def _consume(cls, value: object) -> None:
        cls._blackhole = (31 * cls._blackhole + hash(repr(value))) & 0xFFFFFFFF


class AlgorithmTestRunner:
    """Console reporter for local scripts and CI jobs."""

    @staticmethod
    def print_report(*results: AlgorithmSuiteResult[object]) -> bool:
        all_passed = True
        for result in results:
            all_passed = all_passed and result.passed
            print(AlgorithmTestRunner.format(result))
        return all_passed

    @staticmethod
    def run_and_exit(*results: AlgorithmSuiteResult[object]) -> None:
        if not AlgorithmTestRunner.print_report(*results):
            raise SystemExit(1)

    @staticmethod
    def all_passed(*results: AlgorithmSuiteResult[object]) -> bool:
        return all(result.passed for result in results)

    @staticmethod
    def format(result: AlgorithmSuiteResult[object]) -> str:
        lines = [
            "Suite: "
            f"{result.suite_name} | Algorithm: {result.algorithm_name} | "
            f"{result.passed_case_count}/{len(result.case_results)} passed"
        ]
        for case_result in result.case_results:
            status = "PASS" if case_result.passed else "FAIL"
            line = f"  [{status}] {case_result.case_name}"
            if case_result.stats is not None:
                stats = case_result.stats
                line += (
                    f" | avg {stats.average_duration_ns}ns"
                    f" | min {stats.min_duration_ns}ns"
                    f" | max {stats.max_duration_ns}ns"
                    f" | max memory delta {stats.max_memory_delta_bytes} bytes"
                )
            if not case_result.correct:
                line += f" | expected={case_result.expected!r} actual={case_result.actual!r}"
            if case_result.budget_violations:
                line += f" | budgets={list(case_result.budget_violations)!r}"
            if case_result.error is not None:
                line += (
                    f" | error={case_result.error.__class__.__name__}: "
                    f"{case_result.error}"
                )
            lines.append(line)
        return "\n".join(lines)
