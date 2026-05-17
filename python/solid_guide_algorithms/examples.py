from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .agnostic import adapt_in_place_list_algorithm, adapt_out_of_place, int_list, integer, load_cases
from .framework import (
    Algorithm,
    AlgorithmTestRunner,
    AlgorithmTestSuite,
    BenchmarkOptions,
)


def insertion_sort(values: list[int]) -> list[int]:
    result = list(values)
    insertion_sort_in_place(result)
    return result


def insertion_sort_in_place(values: list[int]) -> None:
    for index in range(1, len(values)):
        current = values[index]
        cursor = index - 1
        while cursor >= 0 and values[cursor] > current:
            values[cursor + 1] = values[cursor]
            cursor -= 1
        values[cursor + 1] = current


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


def insertion_sort_in_place_suite() -> AlgorithmTestSuite[list[int], list[int]]:
    return _sorting_suite(
        "insertion-sort-in-place",
        adapt_in_place_list_algorithm(insertion_sort_in_place),
    )


def binary_search_suite() -> AlgorithmTestSuite[SearchInput, int]:
    return AlgorithmTestSuite(
        suite_name="search",
        algorithm_name="binary-search",
        algorithm=binary_search,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(
            load_cases("search.tsv"),
            _search_input,
            integer,
        ),
    )


def run_examples() -> None:
    from .problems import problem_suites

    AlgorithmTestRunner.run_and_exit(
        insertion_sort_suite().run(),
        insertion_sort_in_place_suite().run(),
        merge_sort_suite().run(),
        binary_search_suite().run(),
        *(suite.run() for suite in problem_suites()),
    )


def _sorting_suite(
    algorithm_name: str,
    algorithm: Algorithm[list[int], list[int]],
) -> AlgorithmTestSuite[list[int], list[int]]:
    return AlgorithmTestSuite(
        suite_name="sorting",
        algorithm_name=algorithm_name,
        algorithm=algorithm,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("sorting.tsv"), int_list, int_list),
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


def _search_input(value: Any) -> SearchInput:
    if not isinstance(value, dict):
        raise TypeError(f"expected record, got {type(value).__name__}")
    return SearchInput(tuple(int_list(value["values"])), integer(value["target"]))
