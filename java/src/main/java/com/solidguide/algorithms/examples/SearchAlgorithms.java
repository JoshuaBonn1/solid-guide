package com.solidguide.algorithms.examples;

public final class SearchAlgorithms {
    private SearchAlgorithms() {
    }

    public static int binarySearch(SearchInput input) {
        int low = 0;
        int high = input.values().size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            int value = input.values().get(mid);
            if (value == input.target()) {
                return mid;
            }
            if (value < input.target()) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    public static int linearSearch(SearchInput input) {
        for (int i = 0; i < input.values().size(); i++) {
            if (input.values().get(i) == input.target()) {
                return i;
            }
        }
        return -1;
    }
}
