package com.solidguide.algorithms.examples;

import com.solidguide.algorithms.framework.AlgorithmSuiteResult;
import com.solidguide.algorithms.framework.AlgorithmTestRunner;

import java.util.List;

public final class ExampleAlgorithmsTest {
    private ExampleAlgorithmsTest() {
    }

    public static void main(String[] args) {
        verifiesSortingExamples();
        verifiesSearchExample();
        verifiesCodingProblemExamples();
    }

    private static void verifiesSortingExamples() {
        List<AlgorithmSuiteResult<List<Integer>>> results = List.of(
                ExampleHarness.insertionSortSuite().run(),
                ExampleHarness.insertionSortInPlaceSuite().run(),
                ExampleHarness.mergeSortSuite().run());

        for (AlgorithmSuiteResult<List<Integer>> result : results) {
            if (!result.passed()) {
                throw new AssertionError("expected sorting suite to pass\n" + AlgorithmTestRunner.format(result));
            }
        }
    }

    private static void verifiesSearchExample() {
        AlgorithmSuiteResult<Integer> result = ExampleHarness.binarySearchSuite().run();
        if (!result.passed()) {
            throw new AssertionError("expected search suite to pass\n" + AlgorithmTestRunner.format(result));
        }
    }

    private static void verifiesCodingProblemExamples() {
        if (!ExampleHarness.twoSumSuite().run().passed()) {
            throw new AssertionError("expected two-sum suite to pass");
        }
        if (!ExampleHarness.validParenthesesSuite().run().passed()) {
            throw new AssertionError("expected valid-parentheses suite to pass");
        }
        if (!ExampleHarness.numberOfIslandsSuite().run().passed()) {
            throw new AssertionError("expected number-of-islands suite to pass");
        }
        if (!ExampleHarness.courseScheduleSuite().run().passed()) {
            throw new AssertionError("expected course-schedule suite to pass");
        }
        if (!ExampleHarness.trappingRainWaterSuite().run().passed()) {
            throw new AssertionError("expected trapping-rain-water suite to pass");
        }
        if (!ExampleHarness.bestTimeStockSuite().run().passed()) {
            throw new AssertionError("expected best-time-stock suite to pass");
        }
        if (!ExampleHarness.validAnagramSuite().run().passed()) {
            throw new AssertionError("expected valid-anagram suite to pass");
        }
        if (!ExampleHarness.maximumSubarraySuite().run().passed()) {
            throw new AssertionError("expected maximum-subarray suite to pass");
        }
        if (!ExampleHarness.mergeIntervalsSuite().run().passed()) {
            throw new AssertionError("expected merge-intervals suite to pass");
        }
        if (!ExampleHarness.editDistanceSuite().run().passed()) {
            throw new AssertionError("expected edit-distance suite to pass");
        }
    }
}
