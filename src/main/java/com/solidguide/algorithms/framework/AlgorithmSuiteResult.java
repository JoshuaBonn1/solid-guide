package com.solidguide.algorithms.framework;

import java.util.List;

/**
 * Result for an algorithm across all registered cases.
 */
public record AlgorithmSuiteResult<O>(
        String suiteName,
        String algorithmName,
        List<AlgorithmCaseResult<O>> caseResults) {
    public AlgorithmSuiteResult {
        caseResults = List.copyOf(caseResults);
    }

    public boolean passed() {
        return caseResults.stream().allMatch(AlgorithmCaseResult::passed);
    }

    public int passedCaseCount() {
        return (int) caseResults.stream().filter(AlgorithmCaseResult::passed).count();
    }

    public int failedCaseCount() {
        return caseResults.size() - passedCaseCount();
    }
}
