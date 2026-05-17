package com.solidguide.algorithms.framework;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A reproducible input and oracle for one algorithm scenario.
 */
public final class AlgorithmCase<I, O> {
    private final String name;
    private final Supplier<I> inputFactory;
    private final Function<I, O> expectedOutput;
    private final BiPredicate<O, O> equality;
    private final ComplexityBudget budget;

    private AlgorithmCase(
            String name,
            Supplier<I> inputFactory,
            Function<I, O> expectedOutput,
            BiPredicate<O, O> equality,
            ComplexityBudget budget) {
        this.name = requireName(name, "name");
        this.inputFactory = Objects.requireNonNull(inputFactory, "inputFactory");
        this.expectedOutput = Objects.requireNonNull(expectedOutput, "expectedOutput");
        this.equality = Objects.requireNonNull(equality, "equality");
        this.budget = Objects.requireNonNull(budget, "budget");
    }

    public static <I, O> Builder<I, O> builder(String name) {
        return new Builder<>(name);
    }

    public String name() {
        return name;
    }

    public Supplier<I> inputFactory() {
        return inputFactory;
    }

    public Function<I, O> expectedOutput() {
        return expectedOutput;
    }

    public BiPredicate<O, O> equality() {
        return equality;
    }

    public ComplexityBudget budget() {
        return budget;
    }

    static String requireName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public static final class Builder<I, O> {
        private final String name;
        private Supplier<I> inputFactory;
        private Function<I, O> expectedOutput;
        private BiPredicate<O, O> equality = Objects::equals;
        private ComplexityBudget budget = ComplexityBudget.none();

        private Builder(String name) {
            this.name = name;
        }

        public Builder<I, O> input(Supplier<I> inputFactory) {
            this.inputFactory = inputFactory;
            return this;
        }

        public Builder<I, O> expect(Function<I, O> expectedOutput) {
            this.expectedOutput = expectedOutput;
            return this;
        }

        public Builder<I, O> equality(BiPredicate<O, O> equality) {
            this.equality = equality;
            return this;
        }

        public Builder<I, O> budget(ComplexityBudget budget) {
            this.budget = budget;
            return this;
        }

        public AlgorithmCase<I, O> build() {
            return new AlgorithmCase<>(name, inputFactory, expectedOutput, equality, budget);
        }
    }
}
