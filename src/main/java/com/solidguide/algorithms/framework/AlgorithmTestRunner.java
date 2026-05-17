package com.solidguide.algorithms.framework;

import java.util.Arrays;

/**
 * Console reporter for local scripts and CI jobs.
 */
public final class AlgorithmTestRunner {
    private AlgorithmTestRunner() {
    }

    public static boolean printReport(AlgorithmSuiteResult<?>... results) {
        boolean allPassed = true;
        for (AlgorithmSuiteResult<?> result : results) {
            allPassed &= result.passed();
            System.out.println(format(result));
        }
        return allPassed;
    }

    public static void runAndExit(AlgorithmSuiteResult<?>... results) {
        if (!printReport(results)) {
            System.exit(1);
        }
    }

    public static String format(AlgorithmSuiteResult<?> result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Suite: ")
                .append(result.suiteName())
                .append(" | Algorithm: ")
                .append(result.algorithmName())
                .append(" | ")
                .append(result.passedCaseCount())
                .append("/")
                .append(result.caseResults().size())
                .append(" passed")
                .append(System.lineSeparator());

        for (AlgorithmCaseResult<?> caseResult : result.caseResults()) {
            builder.append("  [")
                    .append(caseResult.passed() ? "PASS" : "FAIL")
                    .append("] ")
                    .append(caseResult.caseName());

            caseResult.stats().ifPresent(stats -> builder
                    .append(" | avg ")
                    .append(stats.averageDurationNanos())
                    .append("ns")
                    .append(" | min ")
                    .append(stats.minDurationNanos())
                    .append("ns")
                    .append(" | max ")
                    .append(stats.maxDurationNanos())
                    .append("ns")
                    .append(" | max heap delta ")
                    .append(stats.maxMemoryDeltaBytes())
                    .append(" bytes"));

            if (!caseResult.correct()) {
                builder.append(" | expected=")
                        .append(caseResult.expected())
                        .append(" actual=")
                        .append(caseResult.actual());
            }
            if (!caseResult.budgetViolations().isEmpty()) {
                builder.append(" | budgets=")
                        .append(caseResult.budgetViolations());
            }
            caseResult.error().ifPresent(error -> builder
                    .append(" | error=")
                    .append(error.getClass().getSimpleName())
                    .append(": ")
                    .append(error.getMessage()));
            builder.append(System.lineSeparator());
        }

        return builder.toString();
    }

    public static boolean allPassed(AlgorithmSuiteResult<?>... results) {
        return Arrays.stream(results).allMatch(AlgorithmSuiteResult::passed);
    }
}
