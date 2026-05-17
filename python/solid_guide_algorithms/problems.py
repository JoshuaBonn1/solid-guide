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


def problem_suites() -> tuple[AlgorithmTestSuite[Any, Any], ...]:
    return (
        two_sum_suite(),
        valid_parentheses_suite(),
        number_of_islands_suite(),
        course_schedule_suite(),
        trapping_rain_water_suite(),
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
