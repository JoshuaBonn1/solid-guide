package com.solidguide.algorithms.examples;

import com.solidguide.algorithms.framework.AlgorithmCase;
import com.solidguide.algorithms.framework.AlgorithmSuiteResult;
import com.solidguide.algorithms.framework.AlgorithmTestRunner;
import com.solidguide.algorithms.framework.AlgorithmTestSuite;
import com.solidguide.algorithms.framework.BenchmarkOptions;
import com.solidguide.algorithms.framework.ComplexityBudget;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ExampleHarness {
    private ExampleHarness() {
    }

    public static void main(String[] args) {
        AlgorithmTestRunner.runAndExit(
                insertionSortSuite().run(),
                mergeSortSuite().run(),
                binarySearchSuite().run());
    }

    public static AlgorithmTestSuite<List<Integer>, List<Integer>> insertionSortSuite() {
        return sortingSuite("insertion-sort", SortingAlgorithms::insertionSort);
    }

    public static AlgorithmTestSuite<List<Integer>, List<Integer>> mergeSortSuite() {
        return sortingSuite("merge-sort", SortingAlgorithms::mergeSort);
    }

    public static AlgorithmTestSuite<SearchInput, Integer> binarySearchSuite() {
        return AlgorithmTestSuite.<SearchInput, Integer>builder("search")
                .algorithm("binary-search", SearchAlgorithms::binarySearch)
                .options(BenchmarkOptions.quick())
                .addCase(AlgorithmCase.<SearchInput, Integer>builder("finds first element")
                        .input(() -> new SearchInput(List.of(1, 3, 5, 7, 9), 1))
                        .expect(SearchAlgorithms::linearSearch)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofMillis(2))
                                .maxMemoryDeltaBytes(256 * 1024)
                                .build())
                        .build())
                .addCase(AlgorithmCase.<SearchInput, Integer>builder("finds middle element")
                        .input(() -> new SearchInput(List.of(1, 3, 5, 7, 9), 5))
                        .expect(SearchAlgorithms::linearSearch)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofMillis(2))
                                .maxMemoryDeltaBytes(256 * 1024)
                                .build())
                        .build())
                .addCase(AlgorithmCase.<SearchInput, Integer>builder("reports absent element")
                        .input(() -> new SearchInput(List.of(1, 3, 5, 7, 9), 4))
                        .expect(SearchAlgorithms::linearSearch)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofMillis(2))
                                .maxMemoryDeltaBytes(256 * 1024)
                                .build())
                        .build())
                .build();
    }

    private static AlgorithmTestSuite<List<Integer>, List<Integer>> sortingSuite(
            String algorithmName,
            com.solidguide.algorithms.framework.Algorithm<List<Integer>, List<Integer>> algorithm) {
        ComplexityBudget smallBudget = ComplexityBudget.builder()
                .maxAverageDuration(Duration.ofMillis(5))
                .maxMemoryDeltaBytes(1024 * 1024)
                .build();

        return AlgorithmTestSuite.<List<Integer>, List<Integer>>builder("sorting")
                .algorithm(algorithmName, algorithm)
                .options(BenchmarkOptions.quick())
                .addCase(AlgorithmCase.<List<Integer>, List<Integer>>builder("empty input")
                        .input(List::of)
                        .expect(SortingAlgorithms::javaSort)
                        .budget(smallBudget)
                        .build())
                .addCase(AlgorithmCase.<List<Integer>, List<Integer>>builder("duplicates and negatives")
                        .input(() -> List.of(7, -1, 3, 3, 0, -1, 11))
                        .expect(SortingAlgorithms::javaSort)
                        .budget(smallBudget)
                        .build())
                .addCase(AlgorithmCase.<List<Integer>, List<Integer>>builder("reverse ordered")
                        .input(() -> List.of(9, 8, 7, 6, 5, 4, 3, 2, 1))
                        .expect(SortingAlgorithms::javaSort)
                        .budget(smallBudget)
                        .build())
                .addCase(AlgorithmCase.<List<Integer>, List<Integer>>builder("seeded random sample")
                        .input(() -> randomIntegers(64, 8675309L))
                        .expect(SortingAlgorithms::javaSort)
                        .budget(ComplexityBudget.builder()
                                .maxAverageDuration(Duration.ofMillis(10))
                                .maxMemoryDeltaBytes(2 * 1024 * 1024)
                                .build())
                        .build())
                .build();
    }

    private static List<Integer> randomIntegers(int count, long seed) {
        Random random = new Random(seed);
        List<Integer> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(random.nextInt(10_000) - 5_000);
        }
        return values;
    }
}
