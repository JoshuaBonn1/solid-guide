from __future__ import annotations

import unittest

from solid_guide_algorithms import AlgorithmTestRunner
from solid_guide_algorithms.examples import (
    binary_search_suite,
    insertion_sort_in_place_suite,
    insertion_sort_suite,
    merge_sort_suite,
)
from solid_guide_algorithms.problems import problem_suites


class ExampleAlgorithmsTest(unittest.TestCase):
    def test_sorting_examples_pass(self) -> None:
        for result in (insertion_sort_suite().run(), merge_sort_suite().run()):
            self.assertTrue(result.passed, AlgorithmTestRunner.format(result))

    def test_in_place_sorting_example_passes(self) -> None:
        result = insertion_sort_in_place_suite().run()
        self.assertTrue(result.passed, AlgorithmTestRunner.format(result))

    def test_search_example_passes(self) -> None:
        result = binary_search_suite().run()
        self.assertTrue(result.passed, AlgorithmTestRunner.format(result))

    def test_coding_problem_examples_pass(self) -> None:
        for suite in problem_suites():
            result = suite.run()
            self.assertTrue(result.passed, AlgorithmTestRunner.format(result))


if __name__ == "__main__":
    unittest.main()
