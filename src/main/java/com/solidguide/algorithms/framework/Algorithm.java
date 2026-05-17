package com.solidguide.algorithms.framework;

/**
 * A candidate algorithm under test.
 *
 * @param <I> input type
 * @param <O> output type
 */
@FunctionalInterface
public interface Algorithm<I, O> {
    O run(I input) throws Exception;
}
