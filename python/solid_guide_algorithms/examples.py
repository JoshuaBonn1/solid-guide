from __future__ import annotations

from dataclasses import dataclass
from random import Random

from .framework import (
    Algorithm,
    AlgorithmCase,
    AlgorithmTestRunner,
    AlgorithmTestSuite,
    BenchmarkOptions,
    ComplexityBudget,
)


def insertion_sort(values: list[int]) -> list[int]:
    result = list(values)
    for index in range(1, len(result)):
        current = result[index]
        cursor = index - 1
        while cursor >= 0 and result[cursor] > current:
            result[cursor + 1] = result[cursor]
            cursor -= 1
        result[cursor + 1] = current
    return result


def merge_sort(values: list[int]) -> list[int]:
    if len(values) <= 1:
        return list(values)
    midpoint = len(values) // 2
    return _merge(merge_sort(values[:midpoint]), merge_sort(values[midpoint:]))


def python_sort(values: list[int]) -> list[int]:
    return sorted(values)


@dataclass(frozen=True)
class SearchInput:
    values: tuple[int, ...]
    target: int


def binary_search(input_data: SearchInput) -> int:
    low = 0
    high = len(input_data.values) - 1
    while low <= high:
        mid = low + (high - low) // 2
        value = input_data.values[mid]
        if value == input_data.target:
            return mid
        if value < input_data.target:
            low = mid + 1
        else:
            high = mid - 1
    return -1


def linear_search(input_data: SearchInput) -> int:
    for index, value in enumerate(input_data.values):
        if value == input_data.target:
            return index
    return -1


def insertion_sort_suite() -> AlgorithmTestSuite[list[int], list[int]]:
    return _sorting_suite("insertion-sort", insertion_sort)


def merge_sort_suite() -> AlgorithmTestSuite[list[int], list[int]]:
    return _sorting_suite("merge-sort", merge_sort)


def binary_search_suite() -> AlgorithmTestSuite[SearchInput, int]:
    budget = ComplexityBudget.with_limits(
        max_average_duration_seconds=0.002,
        max_memory_delta_bytes=256 * 1024,
    )
    return AlgorithmTestSuite(
        suite_name="search",
        algorithm_name="binary-search",
        algorithm=binary_search,
        options=BenchmarkOptions.quick(),
        cases=(
            AlgorithmCase(
                name="finds first element",
                input_factory=lambda: SearchInput((1, 3, 5, 7, 9), 1),
                expected_output=linear_search,
                budget=budget,
            ),
            AlgorithmCase(
                name="finds middle element",
                input_factory=lambda: SearchInput((1, 3, 5, 7, 9), 5),
                expected_output=linear_search,
                budget=budget,
            ),
            AlgorithmCase(
                name="reports absent element",
                input_factory=lambda: SearchInput((1, 3, 5, 7, 9), 4),
                expected_output=linear_search,
                budget=budget,
            ),
        ),
    )


def run_examples() -> None:
    AlgorithmTestRunner.run_and_exit(
        insertion_sort_suite().run(),
        merge_sort_suite().run(),
        binary_search_suite().run(),
    )


def _sorting_suite(
    algorithm_name: str,
    algorithm: Algorithm[list[int], list[int]],
) -> AlgorithmTestSuite[list[int], list[int]]:
    small_budget = ComplexityBudget.with_limits(
        max_average_duration_seconds=0.005,
        max_memory_delta_bytes=1024 * 1024,
    )
    return AlgorithmTestSuite(
        suite_name="sorting",
        algorithm_name=algorithm_name,
        algorithm=algorithm,
        options=BenchmarkOptions.quick(),
        cases=(
            AlgorithmCase(
                name="empty input",
                input_factory=list,
                expected_output=python_sort,
                budget=small_budget,
            ),
            AlgorithmCase(
                name="duplicates and negatives",
                input_factory=lambda: [7, -1, 3, 3, 0, -1, 11],
                expected_output=python_sort,
                budget=small_budget,
            ),
            AlgorithmCase(
                name="reverse ordered",
                input_factory=lambda: [9, 8, 7, 6, 5, 4, 3, 2, 1],
                expected_output=python_sort,
                budget=small_budget,
            ),
            AlgorithmCase(
                name="seeded random sample",
                input_factory=lambda: _random_integers(64, 8675309),
                expected_output=python_sort,
                budget=ComplexityBudget.with_limits(
                    max_average_duration_seconds=0.01,
                    max_memory_delta_bytes=2 * 1024 * 1024,
                ),
            ),
        ),
    )


def _merge(left: list[int], right: list[int]) -> list[int]:
    merged: list[int] = []
    left_index = 0
    right_index = 0
    while left_index < len(left) and right_index < len(right):
        if left[left_index] <= right[right_index]:
            merged.append(left[left_index])
            left_index += 1
        else:
            merged.append(right[right_index])
            right_index += 1
    merged.extend(left[left_index:])
    merged.extend(right[right_index:])
    return merged


def _random_integers(count: int, seed: int) -> list[int]:
    random = Random(seed)
    return [random.randrange(-5000, 5000) for _ in range(count)]
