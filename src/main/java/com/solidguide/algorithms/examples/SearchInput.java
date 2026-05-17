package com.solidguide.algorithms.examples;

import java.util.List;

public record SearchInput(List<Integer> values, int target) {
    public SearchInput {
        values = List.copyOf(values);
    }
}
