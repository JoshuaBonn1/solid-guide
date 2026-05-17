const std = @import("std");
const framework = @import("framework.zig");
const agnostic = @import("agnostic.zig");

pub const SearchInput = agnostic.SearchRecord;

pub fn insertionSort(allocator: std.mem.Allocator, input: []const i32) ![]i32 {
    const values = try allocator.alloc(i32, input.len);
    @memcpy(values, input);
    return insertionSortInPlace(allocator, values);
}

pub fn insertionSortInPlace(_: std.mem.Allocator, values: []i32) ![]i32 {
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

pub fn ExampleSuite(comptime Input: type, comptime Output: type) type {
    return struct {
        adapted: agnostic.AdaptedCases(Input, Output),
        suite: framework.AlgorithmTestSuite(Input, Output),

        pub fn deinit(self: @This()) void {
            self.adapted.deinit();
        }

        pub fn run(self: @This()) !framework.AlgorithmSuiteResult {
            return self.suite.run();
        }
    };
}

pub fn insertionSortSuite(allocator: std.mem.Allocator) !ExampleSuite([]const i32, []i32) {
    return sortingSuite(allocator, "insertion-sort", insertionSort);
}

pub fn mergeSortSuite(allocator: std.mem.Allocator) !ExampleSuite([]const i32, []i32) {
    return sortingSuite(allocator, "merge-sort", mergeSort);
}

pub fn insertionSortInPlaceSuite(allocator: std.mem.Allocator) !ExampleSuite([]i32, []i32) {
    const data = try agnostic.loadSortingCases(allocator);
    errdefer allocator.free(data);
    const cases = try agnostic.adaptSortingInPlace(allocator, data);
    errdefer allocator.free(cases);
    const adapted = agnostic.AdaptedCases([]i32, []i32){
        .data = data,
        .cases = cases,
        .allocator = allocator,
    };
    return .{
        .adapted = adapted,
        .suite = .{
            .suite_name = "sorting",
            .algorithm_name = "insertion-sort-in-place",
            .algorithm = insertionSortInPlace,
            .cases = adapted.cases,
            .options = framework.BenchmarkOptions.quick(),
            .allocator = allocator,
        },
    };
}

pub fn binarySearchSuite(allocator: std.mem.Allocator) !ExampleSuite(SearchInput, i32) {
    const data = try agnostic.loadSearchCases(allocator);
    errdefer allocator.free(data);
    const cases = try agnostic.adaptSearch(allocator, data);
    errdefer allocator.free(cases);
    const adapted = agnostic.AdaptedCases(SearchInput, i32){
        .data = data,
        .cases = cases,
        .allocator = allocator,
    };
    return .{
        .adapted = adapted,
        .suite = .{
            .suite_name = "search",
            .algorithm_name = "binary-search",
            .algorithm = binarySearch,
            .cases = adapted.cases,
            .options = framework.BenchmarkOptions.quick(),
            .allocator = allocator,
        },
    };
}

pub fn runExamples(allocator: std.mem.Allocator) !bool {
    var insertion_suite = try insertionSortSuite(allocator);
    defer insertion_suite.deinit();
    var insertion_result = try insertion_suite.run();
    defer insertion_result.deinit();
    var insertion_in_place_suite = try insertionSortInPlaceSuite(allocator);
    defer insertion_in_place_suite.deinit();
    var insertion_in_place_result = try insertion_in_place_suite.run();
    defer insertion_in_place_result.deinit();
    var merge_suite = try mergeSortSuite(allocator);
    defer merge_suite.deinit();
    var merge_result = try merge_suite.run();
    defer merge_result.deinit();
    var binary_suite = try binarySearchSuite(allocator);
    defer binary_suite.deinit();
    var binary_result = try binary_suite.run();
    defer binary_result.deinit();

    const results = [_]framework.AlgorithmSuiteResult{ insertion_result, insertion_in_place_result, merge_result, binary_result };
    return framework.AlgorithmTestRunner.printReport(&results);
}

fn sortingSuite(
    allocator: std.mem.Allocator,
    algorithm_name: []const u8,
    algorithm: framework.Algorithm([]const i32, []i32),
) !ExampleSuite([]const i32, []i32) {
    const data = try agnostic.loadSortingCases(allocator);
    errdefer allocator.free(data);
    const cases = try agnostic.adaptSortingOutOfPlace(allocator, data);
    errdefer allocator.free(cases);
    const adapted = agnostic.AdaptedCases([]const i32, []i32){
        .data = data,
        .cases = cases,
        .allocator = allocator,
    };
    return .{
        .adapted = adapted,
        .suite = .{
            .suite_name = "sorting",
            .algorithm_name = algorithm_name,
            .algorithm = algorithm,
            .cases = adapted.cases,
            .options = framework.BenchmarkOptions.quick(),
            .allocator = allocator,
        },
    };
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
