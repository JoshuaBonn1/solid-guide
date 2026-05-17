package com.solidguide.algorithms.framework;

import java.time.Duration;

public final class FrameworkSelfTest {
    private FrameworkSelfTest() {
    }

    public static void main(String[] args) {
        passingAlgorithmsProduceStats();
        incorrectAlgorithmsFailCorrectness();
        budgetViolationsFailCases();
        thrownExceptionsFailCases();
    }

    private static void passingAlgorithmsProduceStats() {
        AlgorithmSuiteResult<Integer> result = AlgorithmTestSuite.<Integer, Integer>builder("identity")
                .algorithm("returns input", input -> input)
                .options(new BenchmarkOptions(0, 3, false, false))
                .addCase(AlgorithmCase.<Integer, Integer>builder("zero")
                        .input(() -> 0)
                        .expect(input -> input)
                        .build())
                .build()
                .run();

        assertTrue(result.passed(), "identity suite should pass");
        ExecutionStats stats = result.caseResults().get(0).stats().orElseThrow();
        assertEquals(3, stats.iterations(), "measurement iteration count");
    }

    private static void incorrectAlgorithmsFailCorrectness() {
        AlgorithmSuiteResult<Integer> result = AlgorithmTestSuite.<Integer, Integer>builder("math")
                .algorithm("off by one", input -> input + 1)
                .options(BenchmarkOptions.quick().withoutMemoryMeasurements())
                .addCase(AlgorithmCase.<Integer, Integer>builder("same value")
                        .input(() -> 41)
                        .expect(input -> input)
                        .build())
                .build()
                .run();

        AlgorithmCaseResult<Integer> caseResult = result.caseResults().get(0);
        assertFalse(result.passed(), "incorrect suite should fail");
        assertFalse(caseResult.correct(), "case should fail correctness");
        assertTrue(caseResult.stats().isEmpty(), "incorrect cases should skip measurements");
    }

    private static void budgetViolationsFailCases() {
        AlgorithmSuiteResult<Integer> result = AlgorithmTestSuite.<Integer, Integer>builder("budget")
                .algorithm("too slow", input -> {
                    Thread.sleep(2L);
                    return input;
                })
                .options(new BenchmarkOptions(0, 1, false, false))
                .addCase(AlgorithmCase.<Integer, Integer>builder("duration budget")
                        .input(() -> 1)
                        .expect(input -> input)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofNanos(1))
                                .build())
                        .build())
                .build()
                .run();

        AlgorithmCaseResult<Integer> caseResult = result.caseResults().get(0);
        assertFalse(result.passed(), "budget suite should fail");
        assertTrue(caseResult.correct(), "budget failure should still be correct");
        assertFalse(caseResult.budgetViolations().isEmpty(), "budget violation should be recorded");
    }

    private static void thrownExceptionsFailCases() {
        AlgorithmSuiteResult<Integer> result = AlgorithmTestSuite.<Integer, Integer>builder("errors")
                .algorithm("throws", input -> {
                    throw new IllegalStateException("boom");
                })
                .options(BenchmarkOptions.quick().withoutMemoryMeasurements())
                .addCase(AlgorithmCase.<Integer, Integer>builder("exception")
                        .input(() -> 1)
                        .expect(input -> input)
                        .build())
                .build()
                .run();

        AlgorithmCaseResult<Integer> caseResult = result.caseResults().get(0);
        assertFalse(result.passed(), "throwing suite should fail");
        assertTrue(caseResult.error().isPresent(), "error should be captured");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
