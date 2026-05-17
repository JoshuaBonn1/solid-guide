"""Python implementation of the solid-guide algorithm testing framework."""

from .framework import (
    AlgorithmCase,
    AlgorithmCaseResult,
    AlgorithmSuiteResult,
    AlgorithmTestRunner,
    AlgorithmTestSuite,
    BenchmarkOptions,
    ComplexityBudget,
    ExecutionStats,
)
from .agnostic import AgnosticCase, adapt_in_place_list_algorithm, adapt_out_of_place, load_cases

__all__ = [
    "AgnosticCase",
    "AlgorithmCase",
    "AlgorithmCaseResult",
    "AlgorithmSuiteResult",
    "AlgorithmTestRunner",
    "AlgorithmTestSuite",
    "BenchmarkOptions",
    "ComplexityBudget",
    "ExecutionStats",
    "adapt_in_place_list_algorithm",
    "adapt_out_of_place",
    "load_cases",
]
