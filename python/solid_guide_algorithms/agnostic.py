from __future__ import annotations

from collections.abc import Callable, Iterable
from dataclasses import dataclass
from pathlib import Path
from typing import Any, TypeVar
import json

from .framework import Algorithm, AlgorithmCase, ComplexityBudget

I = TypeVar("I")
O = TypeVar("O")


@dataclass(frozen=True)
class AgnosticCase:
    name: str
    input_value: Any
    output_value: Any
    max_average_duration_ns: int | None
    max_memory_delta_bytes: int | None

    @property
    def budget(self) -> ComplexityBudget:
        return ComplexityBudget(
            max_average_duration_ns=self.max_average_duration_ns,
            max_memory_delta_bytes=self.max_memory_delta_bytes,
        )


def load_cases(case_file: str | Path) -> tuple[AgnosticCase, ...]:
    path = _resolve_case_file(case_file)
    cases: list[AgnosticCase] = []
    header_seen = False

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if not header_seen:
            header_seen = True
            continue

        name, input_text, output_text, max_duration, max_memory = raw_line.split("\t")
        cases.append(
            AgnosticCase(
                name=name,
                input_value=json.loads(input_text),
                output_value=json.loads(output_text),
                max_average_duration_ns=_optional_int(max_duration),
                max_memory_delta_bytes=_optional_int(max_memory),
            )
        )

    if not cases:
        raise ValueError(f"no cases found in {path}")
    return tuple(cases)


def adapt_out_of_place(
    cases: Iterable[AgnosticCase],
    input_adapter: Callable[[Any], I],
    output_adapter: Callable[[Any], O],
) -> tuple[AlgorithmCase[I, O], ...]:
    return tuple(
        AlgorithmCase(
            name=case.name,
            input_factory=lambda case=case: input_adapter(case.input_value),
            expected_output=lambda _input, case=case: output_adapter(case.output_value),
            budget=case.budget,
        )
        for case in cases
    )


def adapt_in_place_list_algorithm(
    mutator: Callable[[list[int]], None],
) -> Algorithm[list[int], list[int]]:
    def algorithm(values: list[int]) -> list[int]:
        mutator(values)
        return values

    return algorithm


def int_list(value: Any) -> list[int]:
    if not isinstance(value, list):
        raise TypeError(f"expected list, got {type(value).__name__}")
    return [int(item) for item in value]


def int_matrix(value: Any) -> list[list[int]]:
    if not isinstance(value, list):
        raise TypeError(f"expected matrix, got {type(value).__name__}")
    return [int_list(row) for row in value]


def integer(value: Any) -> int:
    if not isinstance(value, int):
        raise TypeError(f"expected int, got {type(value).__name__}")
    return value


def boolean(value: Any) -> bool:
    if not isinstance(value, bool):
        raise TypeError(f"expected bool, got {type(value).__name__}")
    return value


def string(value: Any) -> str:
    if not isinstance(value, str):
        raise TypeError(f"expected str, got {type(value).__name__}")
    return value


def _optional_int(value: str) -> int | None:
    return None if value == "" else int(value)


def _resolve_case_file(case_file: str | Path) -> Path:
    path = Path(case_file)
    if path.exists():
        return path

    root_path = Path(__file__).resolve().parents[2] / "cases" / path
    if root_path.exists():
        return root_path

    raise FileNotFoundError(case_file)
