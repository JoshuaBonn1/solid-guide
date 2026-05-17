const std = @import("std");
const framework = @import("framework.zig");

pub const SearchInput = struct {
    values: []const i32,
    target: i32,
};

const SortCase = framework.AlgorithmCase([]const i32, []i32);
const SearchCase = framework.AlgorithmCase(SearchInput, i32);

pub fn insertionSort(allocator: std.mem.Allocator, input: []const i32) ![]i32 {
    const values = try allocator.alloc(i32, input.len);
    @memcpy(values, input);

    var index: usize = 1;
    while (index < values.len) : (index += 1) {
        const current = values[index];
        var cursor = index;
        while (cursor > 0 and values[cursor - 1] > current) : (cursor -= 1) {
            values[cursor] = values[cursor - 1];
        }
        values[cursor] = current;
    }

    return values;
}

pub fn mergeSort(allocator: std.mem.Allocator, input: []const i32) ![]i32 {
    if (input.len <= 1) {
        const values = try allocator.alloc(i32, input.len);
        @memcpy(values, input);
        return values;
    }

    const midpoint = input.len / 2;
    const left = try mergeSort(allocator, input[0..midpoint]);
    const right = try mergeSort(allocator, input[midpoint..]);
    return merge(allocator, left, right);
}

pub fn zigSort(allocator: std.mem.Allocator, input: []const i32) ![]i32 {
    const values = try allocator.alloc(i32, input.len);
    @memcpy(values, input);
    std.sort.heap(i32, values, {}, comptime std.sort.asc(i32));
    return values;
}

pub fn binarySearch(_: std.mem.Allocator, input: SearchInput) !i32 {
    var low: usize = 0;
    var high: usize = input.values.len;
    while (low < high) {
        const mid = low + (high - low) / 2;
        const value = input.values[mid];
        if (value == input.target) {
            return @as(i32, @intCast(mid));
        }
        if (value < input.target) {
            low = mid + 1;
        } else {
            high = mid;
        }
    }
    return -1;
}

pub fn linearSearch(_: std.mem.Allocator, input: SearchInput) !i32 {
    for (input.values, 0..) |value, index| {
        if (value == input.target) {
            return @as(i32, @intCast(index));
        }
    }
    return -1;
}

pub fn insertionSortSuite(allocator: std.mem.Allocator) framework.AlgorithmTestSuite([]const i32, []i32) {
    return sortingSuite(allocator, "insertion-sort", insertionSort);
}

pub fn mergeSortSuite(allocator: std.mem.Allocator) framework.AlgorithmTestSuite([]const i32, []i32) {
    return sortingSuite(allocator, "merge-sort", mergeSort);
}

pub fn binarySearchSuite(allocator: std.mem.Allocator) framework.AlgorithmTestSuite(SearchInput, i32) {
    const cases = struct {
        const values = [_]SearchCase{
            .{
                .name = "finds first element",
                .input_factory = firstElementInput,
                .expected_output = linearSearch,
                .equality = intEquality,
                .budget = framework.ComplexityBudget.withLimits(2_000_000, 256 * 1024),
            },
            .{
                .name = "finds middle element",
                .input_factory = middleElementInput,
                .expected_output = linearSearch,
                .equality = intEquality,
                .budget = framework.ComplexityBudget.withLimits(2_000_000, 256 * 1024),
            },
            .{
                .name = "reports absent element",
                .input_factory = absentElementInput,
                .expected_output = linearSearch,
                .equality = intEquality,
                .budget = framework.ComplexityBudget.withLimits(2_000_000, 256 * 1024),
            },
        };
    }.values;

    return .{
        .suite_name = "search",
        .algorithm_name = "binary-search",
        .algorithm = binarySearch,
        .cases = &cases,
        .options = framework.BenchmarkOptions.quick(),
        .allocator = allocator,
    };
}

pub fn runExamples(allocator: std.mem.Allocator, writer: anytype) !bool {
    var insertion_result = try insertionSortSuite(allocator).run();
    defer insertion_result.deinit();
    var merge_result = try mergeSortSuite(allocator).run();
    defer merge_result.deinit();
    var binary_result = try binarySearchSuite(allocator).run();
    defer binary_result.deinit();

    const results = [_]framework.AlgorithmSuiteResult{ insertion_result, merge_result, binary_result };
    return framework.AlgorithmTestRunner.printReport(writer, &results);
}

fn sortingSuite(
    allocator: std.mem.Allocator,
    algorithm_name: []const u8,
    algorithm: framework.Algorithm([]const i32, []i32),
) framework.AlgorithmTestSuite([]const i32, []i32) {
    const cases = struct {
        const values = [_]SortCase{
            .{
                .name = "empty input",
                .input_factory = emptyInput,
                .expected_output = zigSort,
                .equality = sliceEquality,
                .budget = framework.ComplexityBudget.withLimits(5_000_000, 1024 * 1024),
            },
            .{
                .name = "duplicates and negatives",
                .input_factory = duplicatesAndNegativesInput,
                .expected_output = zigSort,
                .equality = sliceEquality,
                .budget = framework.ComplexityBudget.withLimits(5_000_000, 1024 * 1024),
            },
            .{
                .name = "reverse ordered",
                .input_factory = reverseOrderedInput,
                .expected_output = zigSort,
                .equality = sliceEquality,
                .budget = framework.ComplexityBudget.withLimits(5_000_000, 1024 * 1024),
            },
            .{
                .name = "seeded random sample",
                .input_factory = seededRandomInput,
                .expected_output = zigSort,
                .equality = sliceEquality,
                .budget = framework.ComplexityBudget.withLimits(10_000_000, 2 * 1024 * 1024),
            },
        };
    }.values;

    return .{
        .suite_name = "sorting",
        .algorithm_name = algorithm_name,
        .algorithm = algorithm,
        .cases = &cases,
        .options = framework.BenchmarkOptions.quick(),
        .allocator = allocator,
    };
}

fn emptyInput(_: std.mem.Allocator) ![]const i32 {
    return &[_]i32{};
}

fn duplicatesAndNegativesInput(_: std.mem.Allocator) ![]const i32 {
    return &[_]i32{ 7, -1, 3, 3, 0, -1, 11 };
}

fn reverseOrderedInput(_: std.mem.Allocator) ![]const i32 {
    return &[_]i32{ 9, 8, 7, 6, 5, 4, 3, 2, 1 };
}

fn seededRandomInput(allocator: std.mem.Allocator) ![]const i32 {
    var random = std.Random.DefaultPrng.init(8675309);
    const values = try allocator.alloc(i32, 64);
    for (values) |*value| {
        value.* = random.random().intRangeLessThan(i32, -5000, 5000);
    }
    return values;
}

fn firstElementInput(_: std.mem.Allocator) !SearchInput {
    return .{ .values = &[_]i32{ 1, 3, 5, 7, 9 }, .target = 1 };
}

fn middleElementInput(_: std.mem.Allocator) !SearchInput {
    return .{ .values = &[_]i32{ 1, 3, 5, 7, 9 }, .target = 5 };
}

fn absentElementInput(_: std.mem.Allocator) !SearchInput {
    return .{ .values = &[_]i32{ 1, 3, 5, 7, 9 }, .target = 4 };
}

fn merge(allocator: std.mem.Allocator, left: []const i32, right: []const i32) ![]i32 {
    const merged = try allocator.alloc(i32, left.len + right.len);
    var left_index: usize = 0;
    var right_index: usize = 0;
    var merged_index: usize = 0;

    while (left_index < left.len and right_index < right.len) {
        if (left[left_index] <= right[right_index]) {
            merged[merged_index] = left[left_index];
            left_index += 1;
        } else {
            merged[merged_index] = right[right_index];
            right_index += 1;
        }
        merged_index += 1;
    }
    while (left_index < left.len) : (left_index += 1) {
        merged[merged_index] = left[left_index];
        merged_index += 1;
    }
    while (right_index < right.len) : (right_index += 1) {
        merged[merged_index] = right[right_index];
        merged_index += 1;
    }

    return merged;
}

fn sliceEquality(expected: []i32, actual: []i32) bool {
    return std.mem.eql(i32, expected, actual);
}

fn intEquality(expected: i32, actual: i32) bool {
    return expected == actual;
}
