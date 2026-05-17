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
}
