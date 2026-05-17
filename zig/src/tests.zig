const std = @import("std");
const framework = @import("framework.zig");
const examples = @import("examples.zig");
const problems = @import("problems.zig");

const IntCase = framework.AlgorithmCase(i32, i32);

test "passing algorithms produce stats" {
    const cases = [_]IntCase{
        .{
            .name = "zero",
            .input_factory = zeroInput,
            .expected_output = identityExpected,
            .equality = intEquality,
        },
    };

    const suite = framework.AlgorithmTestSuite(i32, i32){
        .suite_name = "identity",
        .algorithm_name = "returns input",
        .algorithm = identityAlgorithm,
        .cases = &cases,
        .options = .{
            .warmup_iterations = 0,
            .measurement_iterations = 3,
            .measure_memory = false,
            .gc_before_memory_measurement = false,
        },
        .allocator = std.testing.allocator,
    };

    var result = try suite.run();
    defer result.deinit();

    try std.testing.expect(result.passed());
    try std.testing.expectEqual(@as(usize, 3), result.case_results[0].stats.?.iterations);
}

test "incorrect algorithms fail correctness" {
    const cases = [_]IntCase{
        .{
            .name = "same value",
            .input_factory = fortyOneInput,
            .expected_output = identityExpected,
            .equality = intEquality,
        },
    };

    const suite = framework.AlgorithmTestSuite(i32, i32){
        .suite_name = "math",
        .algorithm_name = "off by one",
        .algorithm = offByOne,
        .cases = &cases,
        .options = framework.BenchmarkOptions.quick().withoutMemoryMeasurements(),
        .allocator = std.testing.allocator,
    };

    var result = try suite.run();
    defer result.deinit();

    try std.testing.expect(!result.passed());
    try std.testing.expect(!result.case_results[0].correct);
    try std.testing.expect(result.case_results[0].stats == null);
}

test "budget violations fail cases" {
    const cases = [_]IntCase{
        .{
            .name = "duration budget",
            .input_factory = oneInput,
            .expected_output = identityExpected,
            .equality = intEquality,
            .budget = framework.ComplexityBudget.withLimits(1, null),
        },
    };

    const suite = framework.AlgorithmTestSuite(i32, i32){
        .suite_name = "budget",
        .algorithm_name = "too slow",
        .algorithm = slowIdentity,
        .cases = &cases,
        .options = .{
            .warmup_iterations = 0,
            .measurement_iterations = 1,
            .measure_memory = false,
            .gc_before_memory_measurement = false,
        },
        .allocator = std.testing.allocator,
    };

    var result = try suite.run();
    defer result.deinit();

    try std.testing.expect(!result.passed());
    try std.testing.expect(result.case_results[0].correct);
    try std.testing.expect(result.case_results[0].budget_violation_count > 0);
}

test "thrown errors fail cases" {
    const cases = [_]IntCase{
        .{
            .name = "error",
            .input_factory = oneInput,
            .expected_output = identityExpected,
            .equality = intEquality,
        },
    };

    const suite = framework.AlgorithmTestSuite(i32, i32){
        .suite_name = "errors",
        .algorithm_name = "throws",
        .algorithm = brokenAlgorithm,
        .cases = &cases,
        .options = framework.BenchmarkOptions.quick().withoutMemoryMeasurements(),
        .allocator = std.testing.allocator,
    };

    var result = try suite.run();
    defer result.deinit();

    try std.testing.expect(!result.passed());
    try std.testing.expect(result.case_results[0].error_name != null);
}

test "example sorting suites pass" {
    var insertion_suite = try examples.insertionSortSuite(std.testing.allocator);
    defer insertion_suite.deinit();
    var insertion_result = try insertion_suite.run();
    defer insertion_result.deinit();
    var insertion_in_place_suite = try examples.insertionSortInPlaceSuite(std.testing.allocator);
    defer insertion_in_place_suite.deinit();
    var insertion_in_place_result = try insertion_in_place_suite.run();
    defer insertion_in_place_result.deinit();
    var merge_suite = try examples.mergeSortSuite(std.testing.allocator);
    defer merge_suite.deinit();
    var merge_result = try merge_suite.run();
    defer merge_result.deinit();

    try std.testing.expect(insertion_result.passed());
    try std.testing.expect(insertion_in_place_result.passed());
    try std.testing.expect(merge_result.passed());
}

test "example search suite passes" {
    var suite = try examples.binarySearchSuite(std.testing.allocator);
    defer suite.deinit();
    var result = try suite.run();
    defer result.deinit();

    try std.testing.expect(result.passed());
}

test "coding problem suites pass" {
    var two_sum_suite = try problems.twoSumSuite(std.testing.allocator);
    defer two_sum_suite.deinit();
    var two_sum_result = try two_sum_suite.run();
    defer two_sum_result.deinit();
    var valid_suite = try problems.validParenthesesSuite(std.testing.allocator);
    defer valid_suite.deinit();
    var valid_result = try valid_suite.run();
    defer valid_result.deinit();
    var islands_suite = try problems.numberOfIslandsSuite(std.testing.allocator);
    defer islands_suite.deinit();
    var islands_result = try islands_suite.run();
    defer islands_result.deinit();
    var courses_suite = try problems.courseScheduleSuite(std.testing.allocator);
    defer courses_suite.deinit();
    var courses_result = try courses_suite.run();
    defer courses_result.deinit();
    var rain_suite = try problems.trappingRainWaterSuite(std.testing.allocator);
    defer rain_suite.deinit();
    var rain_result = try rain_suite.run();
    defer rain_result.deinit();

    try std.testing.expect(two_sum_result.passed());
    try std.testing.expect(valid_result.passed());
    try std.testing.expect(islands_result.passed());
    try std.testing.expect(courses_result.passed());
    try std.testing.expect(rain_result.passed());
}

fn zeroInput(_: std.mem.Allocator, _: ?*const anyopaque) !i32 {
    return 0;
}

fn oneInput(_: std.mem.Allocator, _: ?*const anyopaque) !i32 {
    return 1;
}

fn fortyOneInput(_: std.mem.Allocator, _: ?*const anyopaque) !i32 {
    return 41;
}

fn identityAlgorithm(_: std.mem.Allocator, value: i32) !i32 {
    return value;
}

fn identityExpected(_: std.mem.Allocator, value: i32, _: ?*const anyopaque) !i32 {
    return value;
}

fn offByOne(_: std.mem.Allocator, value: i32) !i32 {
    return value + 1;
}

fn slowIdentity(_: std.mem.Allocator, value: i32) !i32 {
    var accumulator: usize = 0;
    for (0..10_000) |index| {
        accumulator +%= index;
    }
    std.mem.doNotOptimizeAway(accumulator);
    return value;
}

fn brokenAlgorithm(_: std.mem.Allocator, _: i32) !i32 {
    return error.Boom;
}

fn intEquality(expected: i32, actual: i32) bool {
    return expected == actual;
}
