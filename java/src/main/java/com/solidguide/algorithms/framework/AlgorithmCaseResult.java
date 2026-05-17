package com.solidguide.algorithms.framework;

import java.util.List;
import java.util.Optional;

/**
 * Result for one case after correctness, speed, and memory checks.
 */
public record AlgorithmCaseResult<O>(
        String caseName,
        boolean correct,
        O expected,
        O actual,
        Optional<ExecutionStats> stats,
        List<String> budgetViolations,
        Optional<Throwable> error) {
    public AlgorithmCaseResult {
        budgetViolations = List.copyOf(budgetViolations);
    }

    public boolean passed() {
        return correct && budgetViolations.isEmpty() && error.isEmpty();
    }
}
