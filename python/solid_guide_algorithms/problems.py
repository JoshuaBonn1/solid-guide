from __future__ import annotations

from collections import defaultdict, deque
from dataclasses import dataclass
from typing import Any

from .agnostic import adapt_out_of_place, boolean, int_list, int_matrix, integer, load_cases, string
from .framework import AlgorithmCase, AlgorithmTestSuite, BenchmarkOptions


@dataclass(frozen=True)
class TwoSumInput:
    nums: tuple[int, ...]
    target: int


@dataclass(frozen=True)
class CourseScheduleInput:
    num_courses: int
    prerequisites: tuple[tuple[int, int], ...]


@dataclass(frozen=True)
class AnagramInput:
    s: str
    t: str


@dataclass(frozen=True)
class EditDistanceInput:
    word1: str
    word2: str


def two_sum(input_data: TwoSumInput) -> list[int]:
    seen: dict[int, int] = {}
    for index, value in enumerate(input_data.nums):
        complement = input_data.target - value
        if complement in seen:
            return [seen[complement], index]
        seen[value] = index
    raise ValueError("no two-sum solution")


def valid_parentheses(text: str) -> bool:
    pairs = {")": "(", "]": "[", "}": "{"}
    stack: list[str] = []
    for char in text:
        if char in "([{":
            stack.append(char)
        elif not stack or stack.pop() != pairs.get(char):
            return False
    return not stack


def number_of_islands(grid: list[list[int]]) -> int:
    if not grid or not grid[0]:
        return 0

    rows = len(grid)
    cols = len(grid[0])
    visited = [[False] * cols for _ in range(rows)]
    islands = 0

    def flood_fill(row: int, col: int) -> None:
        if row < 0 or col < 0 or row >= rows or col >= cols:
            return
        if visited[row][col] or grid[row][col] == 0:
            return
        visited[row][col] = True
        flood_fill(row + 1, col)
        flood_fill(row - 1, col)
        flood_fill(row, col + 1)
        flood_fill(row, col - 1)

    for row in range(rows):
        for col in range(cols):
            if grid[row][col] == 1 and not visited[row][col]:
                islands += 1
                flood_fill(row, col)
    return islands


def can_finish_courses(input_data: CourseScheduleInput) -> bool:
    graph: dict[int, list[int]] = defaultdict(list)
    indegree = [0] * input_data.num_courses
    for course, required in input_data.prerequisites:
        graph[required].append(course)
        indegree[course] += 1

    ready = deque(index for index, degree in enumerate(indegree) if degree == 0)
    completed = 0
    while ready:
        course = ready.popleft()
        completed += 1
        for next_course in graph[course]:
            indegree[next_course] -= 1
            if indegree[next_course] == 0:
                ready.append(next_course)
    return completed == input_data.num_courses


def trap_rain_water(heights: list[int]) -> int:
    left = 0
    right = len(heights) - 1
    left_max = 0
    right_max = 0
    water = 0
    while left < right:
        if heights[left] < heights[right]:
            left_max = max(left_max, heights[left])
            water += left_max - heights[left]
            left += 1
        else:
            right_max = max(right_max, heights[right])
            water += right_max - heights[right]
            right -= 1
    return water


def max_profit(prices: list[int]) -> int:
    min_price = 10**18
    best_profit = 0
    for price in prices:
        min_price = min(min_price, price)
        best_profit = max(best_profit, price - min_price)
    return best_profit


def valid_anagram(input_data: AnagramInput) -> bool:
    if len(input_data.s) != len(input_data.t):
        return False
    counts = [0] * 26
    for left, right in zip(input_data.s, input_data.t):
        counts[ord(left) - ord("a")] += 1
        counts[ord(right) - ord("a")] -= 1
    return all(count == 0 for count in counts)


def maximum_subarray(nums: list[int]) -> int:
    current = nums[0]
    best = nums[0]
    for value in nums[1:]:
        current = max(value, current + value)
        best = max(best, current)
    return best


def merge_intervals(intervals: list[list[int]]) -> list[list[int]]:
    if not intervals:
        return []
    sorted_intervals = sorted(intervals, key=lambda interval: interval[0])
    merged = [list(sorted_intervals[0])]
    for start, end in sorted_intervals[1:]:
        if start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    return merged


def edit_distance(input_data: EditDistanceInput) -> int:
    rows = len(input_data.word1)
    cols = len(input_data.word2)
    dp = [[0] * (cols + 1) for _ in range(rows + 1)]
    for row in range(rows + 1):
        dp[row][0] = row
    for col in range(cols + 1):
        dp[0][col] = col
    for row in range(1, rows + 1):
        for col in range(1, cols + 1):
            if input_data.word1[row - 1] == input_data.word2[col - 1]:
                dp[row][col] = dp[row - 1][col - 1]
            else:
                dp[row][col] = 1 + min(
                    dp[row - 1][col - 1],
                    dp[row - 1][col],
                    dp[row][col - 1],
                )
    return dp[rows][cols]


def two_sum_suite() -> AlgorithmTestSuite[TwoSumInput, list[int]]:
    return AlgorithmTestSuite(
        suite_name="two-sum",
        algorithm_name="hash-map-two-sum",
        algorithm=two_sum,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("two_sum.tsv"), _two_sum_input, int_list),
    )


def valid_parentheses_suite() -> AlgorithmTestSuite[str, bool]:
    return AlgorithmTestSuite(
        suite_name="valid-parentheses",
        algorithm_name="stack-validation",
        algorithm=valid_parentheses,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("valid_parentheses.tsv"), string, boolean),
    )


def number_of_islands_suite() -> AlgorithmTestSuite[list[list[int]], int]:
    return AlgorithmTestSuite(
        suite_name="number-of-islands",
        algorithm_name="dfs-grid-traversal",
        algorithm=number_of_islands,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("number_of_islands.tsv"), int_matrix, integer),
    )


def course_schedule_suite() -> AlgorithmTestSuite[CourseScheduleInput, bool]:
    return AlgorithmTestSuite(
        suite_name="course-schedule",
        algorithm_name="topological-sort",
        algorithm=can_finish_courses,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("course_schedule.tsv"), _course_schedule_input, boolean),
    )


def trapping_rain_water_suite() -> AlgorithmTestSuite[list[int], int]:
    return AlgorithmTestSuite(
        suite_name="trapping-rain-water",
        algorithm_name="two-pointer-scan",
        algorithm=trap_rain_water,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("trapping_rain_water.tsv"), int_list, integer),
    )


def best_time_stock_suite() -> AlgorithmTestSuite[list[int], int]:
    return AlgorithmTestSuite(
        suite_name="best-time-stock",
        algorithm_name="one-pass-min-price",
        algorithm=max_profit,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("best_time_stock.tsv"), int_list, integer),
    )


def valid_anagram_suite() -> AlgorithmTestSuite[AnagramInput, bool]:
    return AlgorithmTestSuite(
        suite_name="valid-anagram",
        algorithm_name="frequency-count",
        algorithm=valid_anagram,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("valid_anagram.tsv"), _anagram_input, boolean),
    )


def maximum_subarray_suite() -> AlgorithmTestSuite[list[int], int]:
    return AlgorithmTestSuite(
        suite_name="maximum-subarray",
        algorithm_name="kadane-scan",
        algorithm=maximum_subarray,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("maximum_subarray.tsv"), int_list, integer),
    )


def merge_intervals_suite() -> AlgorithmTestSuite[list[list[int]], list[list[int]]]:
    return AlgorithmTestSuite(
        suite_name="merge-intervals",
        algorithm_name="sort-and-merge",
        algorithm=merge_intervals,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("merge_intervals.tsv"), int_matrix, int_matrix),
    )


def edit_distance_suite() -> AlgorithmTestSuite[EditDistanceInput, int]:
    return AlgorithmTestSuite(
        suite_name="edit-distance",
        algorithm_name="dynamic-programming",
        algorithm=edit_distance,
        options=BenchmarkOptions.quick(),
        cases=adapt_out_of_place(load_cases("edit_distance.tsv"), _edit_distance_input, integer),
    )


def problem_suites() -> tuple[AlgorithmTestSuite[Any, Any], ...]:
    return (
        two_sum_suite(),
        valid_parentheses_suite(),
        number_of_islands_suite(),
        course_schedule_suite(),
        trapping_rain_water_suite(),
        best_time_stock_suite(),
        valid_anagram_suite(),
        maximum_subarray_suite(),
        merge_intervals_suite(),
        edit_distance_suite(),
    )


def _two_sum_input(value: Any) -> TwoSumInput:
    if not isinstance(value, dict):
        raise TypeError(f"expected record, got {type(value).__name__}")
    return TwoSumInput(tuple(int_list(value["nums"])), integer(value["target"]))


def _course_schedule_input(value: Any) -> CourseScheduleInput:
    if not isinstance(value, dict):
        raise TypeError(f"expected record, got {type(value).__name__}")
    return CourseScheduleInput(
        integer(value["numCourses"]),
        tuple((row[0], row[1]) for row in int_matrix(value["prerequisites"])),
    )


def _anagram_input(value: Any) -> AnagramInput:
    if not isinstance(value, dict):
        raise TypeError(f"expected record, got {type(value).__name__}")
    return AnagramInput(string(value["s"]), string(value["t"]))


def _edit_distance_input(value: Any) -> EditDistanceInput:
    if not isinstance(value, dict):
        raise TypeError(f"expected record, got {type(value).__name__}")
    return EditDistanceInput(string(value["word1"]), string(value["word2"]))
