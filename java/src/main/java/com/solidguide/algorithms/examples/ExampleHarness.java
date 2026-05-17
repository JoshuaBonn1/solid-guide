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
                binarySearchSuite().run(),
                twoSumSuite().run(),
                validParenthesesSuite().run(),
                numberOfIslandsSuite().run(),
                courseScheduleSuite().run(),
                trappingRainWaterSuite().run());
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

    public static AlgorithmTestSuite<CodingProblems.TwoSumInput, List<Integer>> twoSumSuite() {
        AlgorithmTestSuite.Builder<CodingProblems.TwoSumInput, List<Integer>> builder =
                AlgorithmTestSuite.<CodingProblems.TwoSumInput, List<Integer>>builder("two-sum")
                        .algorithm("hash-map-two-sum", CodingProblems::twoSum)
                        .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("two_sum.tsv"),
                ExampleHarness::twoSumInput,
                AgnosticCases::parseIntList)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    public static AlgorithmTestSuite<String, Boolean> validParenthesesSuite() {
        AlgorithmTestSuite.Builder<String, Boolean> builder =
                AlgorithmTestSuite.<String, Boolean>builder("valid-parentheses")
                        .algorithm("stack-validation", CodingProblems::validParentheses)
                        .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("valid_parentheses.tsv"),
                AgnosticCases::parseString,
                AgnosticCases::parseBoolean)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    public static AlgorithmTestSuite<List<List<Integer>>, Integer> numberOfIslandsSuite() {
        AlgorithmTestSuite.Builder<List<List<Integer>>, Integer> builder =
                AlgorithmTestSuite.<List<List<Integer>>, Integer>builder("number-of-islands")
                        .algorithm("dfs-grid-traversal", CodingProblems::numberOfIslands)
                        .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("number_of_islands.tsv"),
                AgnosticCases::parseIntMatrix,
                AgnosticCases::parseInt)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    public static AlgorithmTestSuite<CodingProblems.CourseScheduleInput, Boolean> courseScheduleSuite() {
        AlgorithmTestSuite.Builder<CodingProblems.CourseScheduleInput, Boolean> builder =
                AlgorithmTestSuite.<CodingProblems.CourseScheduleInput, Boolean>builder("course-schedule")
                        .algorithm("topological-sort", CodingProblems::canFinishCourses)
                        .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("course_schedule.tsv"),
                ExampleHarness::courseScheduleInput,
                AgnosticCases::parseBoolean)) {
            builder.addCase(testCase);
        }
        return builder.build();
    }

    public static AlgorithmTestSuite<List<Integer>, Integer> trappingRainWaterSuite() {
        AlgorithmTestSuite.Builder<List<Integer>, Integer> builder =
                AlgorithmTestSuite.<List<Integer>, Integer>builder("trapping-rain-water")
                        .algorithm("two-pointer-scan", CodingProblems::trapRainWater)
                        .options(BenchmarkOptions.quick());
        for (var testCase : AgnosticCases.adaptOutOfPlace(
                AgnosticCases.load("trapping_rain_water.tsv"),
                AgnosticCases::parseIntList,
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

    private static CodingProblems.TwoSumInput twoSumInput(String value) {
        Map<String, String> record = AgnosticCases.parseFlatRecord(value);
        return new CodingProblems.TwoSumInput(
                AgnosticCases.parseIntList(record.get("nums")),
                AgnosticCases.parseInt(record.get("target")));
    }

    private static CodingProblems.CourseScheduleInput courseScheduleInput(String value) {
        Map<String, String> record = AgnosticCases.parseFlatRecord(value);
        return new CodingProblems.CourseScheduleInput(
                AgnosticCases.parseInt(record.get("numCourses")),
                AgnosticCases.parseIntMatrix(record.get("prerequisites")));
    }
}
