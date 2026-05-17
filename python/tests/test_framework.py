from __future__ import annotations

import time
import unittest

from solid_guide_algorithms import (
    AlgorithmCase,
    AlgorithmTestSuite,
    BenchmarkOptions,
    ComplexityBudget,
)


class FrameworkSelfTest(unittest.TestCase):
    def test_passing_algorithms_produce_stats(self) -> None:
        result = AlgorithmTestSuite(
            suite_name="identity",
            algorithm_name="returns input",
            algorithm=lambda value: value,
            options=BenchmarkOptions(
                warmup_iterations=0,
                measurement_iterations=3,
                measure_memory=False,
                gc_before_memory_measurement=False,
            ),
            cases=(
                AlgorithmCase(
                    name="zero",
                    input_factory=lambda: 0,
                    expected_output=lambda value: value,
                ),
            ),
        ).run()

        self.assertTrue(result.passed)
        self.assertEqual(3, result.case_results[0].stats.iterations)

    def test_incorrect_algorithms_fail_correctness(self) -> None:
        result = AlgorithmTestSuite(
            suite_name="math",
            algorithm_name="off by one",
            algorithm=lambda value: value + 1,
            options=BenchmarkOptions.quick().without_memory_measurements(),
            cases=(
                AlgorithmCase(
                    name="same value",
                    input_factory=lambda: 41,
                    expected_output=lambda value: value,
                ),
            ),
        ).run()

        case_result = result.case_results[0]
        self.assertFalse(result.passed)
        self.assertFalse(case_result.correct)
        self.assertIsNone(case_result.stats)

    def test_budget_violations_fail_cases(self) -> None:
        def slow_identity(value: int) -> int:
            time.sleep(0.002)
            return value

        result = AlgorithmTestSuite(
            suite_name="budget",
            algorithm_name="too slow",
            algorithm=slow_identity,
            options=BenchmarkOptions(
                warmup_iterations=0,
                measurement_iterations=1,
                measure_memory=False,
                gc_before_memory_measurement=False,
            ),
            cases=(
                AlgorithmCase(
                    name="duration budget",
                    input_factory=lambda: 1,
                    expected_output=lambda value: value,
                    budget=ComplexityBudget(max_average_duration_ns=1),
                ),
            ),
        ).run()

        case_result = result.case_results[0]
        self.assertFalse(result.passed)
        self.assertTrue(case_result.correct)
        self.assertGreater(len(case_result.budget_violations), 0)

    def test_thrown_exceptions_fail_cases(self) -> None:
        def broken_algorithm(value: int) -> int:
            raise RuntimeError("boom")

        result = AlgorithmTestSuite(
            suite_name="errors",
            algorithm_name="throws",
            algorithm=broken_algorithm,
            options=BenchmarkOptions.quick().without_memory_measurements(),
            cases=(
                AlgorithmCase(
                    name="exception",
                    input_factory=lambda: 1,
                    expected_output=lambda value: value,
                ),
            ),
        ).run()

        case_result = result.case_results[0]
        self.assertFalse(result.passed)
        self.assertIsNotNone(case_result.error)


if __name__ == "__main__":
    unittest.main()
