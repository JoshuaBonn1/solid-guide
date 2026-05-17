package com.solidguide.algorithms.examples;

import java.util.ArrayList;
import java.util.List;

public final class SortingAlgorithms {
    private SortingAlgorithms() {
    }

    public static List<Integer> insertionSort(List<Integer> input) {
        List<Integer> values = new ArrayList<>(input);
        for (int i = 1; i < values.size(); i++) {
            int current = values.get(i);
            int j = i - 1;
            while (j >= 0 && values.get(j) > current) {
                values.set(j + 1, values.get(j));
                j--;
            }
            values.set(j + 1, current);
        }
        return values;
    }

    public static List<Integer> mergeSort(List<Integer> input) {
        if (input.size() <= 1) {
            return new ArrayList<>(input);
        }
        int midpoint = input.size() / 2;
        List<Integer> left = mergeSort(input.subList(0, midpoint));
        List<Integer> right = mergeSort(input.subList(midpoint, input.size()));
        return merge(left, right);
    }

    public static List<Integer> javaSort(List<Integer> input) {
        List<Integer> values = new ArrayList<>(input);
        values.sort(Integer::compareTo);
        return values;
    }

    private static List<Integer> merge(List<Integer> left, List<Integer> right) {
        List<Integer> merged = new ArrayList<>(left.size() + right.size());
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.size() && rightIndex < right.size()) {
            if (left.get(leftIndex) <= right.get(rightIndex)) {
                merged.add(left.get(leftIndex++));
            } else {
                merged.add(right.get(rightIndex++));
            }
        }
        while (leftIndex < left.size()) {
            merged.add(left.get(leftIndex++));
        }
        while (rightIndex < right.size()) {
            merged.add(right.get(rightIndex++));
        }
        return merged;
    }
}
