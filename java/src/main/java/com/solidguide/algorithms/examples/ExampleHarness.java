package com.solidguide.algorithms.examples;

import com.solidguide.algorithms.framework.AlgorithmSuiteResult;
import com.solidguide.algorithms.framework.AlgorithmTestRunner;
import com.solidguide.algorithms.framework.AlgorithmTestSuite;
import com.solidguide.algorithms.framework.AgnosticCases;
import com.solidguide.algorithms.framework.BenchmarkOptions;

import java.util.List;
import java.util.Map;

public final class ExampleHarness {
    private ExampleHarness() {
    }

    public static void main(String[] args) {
        AlgorithmTestRunner.runAndExit(
                insertionSortSuite().run(),
                insertionSortInPlaceSuite().run(),
                mergeSortSuite().run(),
                binarySearchSuite().run());
    }

    public static AlgorithmTestSuite<List<Integer>, List<Integer>> insertionSortSuite() {
        return sortingSuite("insertion-sort", SortingAlgorithms::insertionSort);
    }

    public static AlgorithmTestSuite<List<Integer>, List<Integer>> mergeSortSuite() {
        return sortingSuite("merge-sort", SortingAlgorithms::mergeSort);
    }

    public static AlgorithmTestSuite<List<Integer>, List<Integer>> insertionSortInPlaceSuite() {
        return sortingSuite(
                "insertion-sort-in-place",
                AgnosticCases.inPlaceListAlgorithm(SortingAlgorithms::insertionSortInPlace));
    }

    public static AlgorithmTestSuite<SearchInput, Integer> binarySearchSuite() {
        AlgorithmTestSuite.Builder<SearchInput, Integer> builder = AlgorithmTestSuite.<SearchInput, Integer>builder("search")
                .algorithm("binary-search", SearchAlgorithms::binarySearch)
                .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("search.tsv"),
                ExampleHarness::searchInput,
                AgnosticCases::parseInt)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    private static AlgorithmTestSuite<List<Integer>, List<Integer>> sortingSuite(
            String algorithmName,
            com.solidguide.algorithms.framework.Algorithm<List<Integer>, List<Integer>> algorithm) {
        AlgorithmTestSuite.Builder<List<Integer>, List<Integer>> builder = AlgorithmTestSuite.<List<Integer>, List<Integer>>builder("sorting")
                .algorithm(algorithmName, algorithm)
                .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("sorting.tsv"),
                AgnosticCases::parseIntList,
                AgnosticCases::parseIntList)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    private static SearchInput searchInput(String value) {
        Map<String, String> record = AgnosticCases.parseFlatRecord(value);
        return new SearchInput(
                AgnosticCases.parseIntList(record.get("values")),
                AgnosticCases.parseInt(record.get("target")));
    }
}
