const std = @import("std");
const framework = @import("framework.zig");
const agnostic = @import("agnostic.zig");

pub const TwoSumInput = struct {
    nums: []const i32,
    target: i32,
};

pub const CourseScheduleInput = struct {
    num_courses: usize,
    prerequisites: []const []const i32,
};

pub fn ProblemSuite(comptime Input: type, comptime Output: type) type {
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

pub fn twoSum(allocator: std.mem.Allocator, input: TwoSumInput) ![]i32 {
    var seen = std.AutoHashMap(i32, usize).init(allocator);
    defer seen.deinit();

    for (input.nums, 0..) |value, index| {
        const complement = input.target - value;
        if (seen.get(complement)) |previous| {
            const result = try allocator.alloc(i32, 2);
            result[0] = @as(i32, @intCast(previous));
            result[1] = @as(i32, @intCast(index));
            return result;
        }
        try seen.put(value, index);
    }
    return error.NoTwoSumSolution;
}

pub fn validParentheses(allocator: std.mem.Allocator, input: []const u8) !bool {
    const stack = try allocator.alloc(u8, input.len);
    var top: usize = 0;
    for (input) |char| {
        switch (char) {
            '(', '[', '{' => {
                stack[top] = char;
                top += 1;
            },
            ')', ']', '}' => {
                if (top == 0) return false;
                top -= 1;
                if (!matches(stack[top], char)) return false;
            },
            else => return false,
        }
    }
    return top == 0;
}

pub fn numberOfIslands(allocator: std.mem.Allocator, grid: []const []const i32) !i32 {
    if (grid.len == 0 or grid[0].len == 0) return 0;
    const rows = grid.len;
    const cols = grid[0].len;
    const visited = try allocator.alloc(bool, rows * cols);
    @memset(visited, false);

    var islands: i32 = 0;
    for (0..rows) |row| {
        for (0..cols) |col| {
            if (grid[row][col] == 1 and !visited[row * cols + col]) {
                islands += 1;
                floodFill(grid, visited, rows, cols, @as(isize, @intCast(row)), @as(isize, @intCast(col)));
            }
        }
    }
    return islands;
}

pub fn canFinishCourses(allocator: std.mem.Allocator, input: CourseScheduleInput) !bool {
    const indegree = try allocator.alloc(usize, input.num_courses);
    @memset(indegree, 0);
    for (input.prerequisites) |edge| {
        indegree[@as(usize, @intCast(edge[0]))] += 1;
    }

    const queue = try allocator.alloc(usize, input.num_courses);
    var head: usize = 0;
    var tail: usize = 0;
    for (indegree, 0..) |degree, course| {
        if (degree == 0) {
            queue[tail] = course;
            tail += 1;
        }
    }

    var completed: usize = 0;
    while (head < tail) {
        const course = queue[head];
        head += 1;
        completed += 1;
        for (input.prerequisites) |edge| {
            const next = @as(usize, @intCast(edge[0]));
            const required = @as(usize, @intCast(edge[1]));
            if (required == course) {
                indegree[next] -= 1;
                if (indegree[next] == 0) {
                    queue[tail] = next;
                    tail += 1;
                }
            }
        }
    }
    return completed == input.num_courses;
}

pub fn trapRainWater(_: std.mem.Allocator, heights: []const i32) !i32 {
    if (heights.len == 0) return 0;
    var left: usize = 0;
    var right: usize = heights.len - 1;
    var left_max: i32 = 0;
    var right_max: i32 = 0;
    var water: i32 = 0;
    while (left < right) {
        if (heights[left] < heights[right]) {
            left_max = @max(left_max, heights[left]);
            water += left_max - heights[left];
            left += 1;
        } else {
            right_max = @max(right_max, heights[right]);
            water += right_max - heights[right];
            right -= 1;
        }
    }
    return water;
}

pub fn twoSumSuite(allocator: std.mem.Allocator) !ProblemSuite(TwoSumInput, []i32) {
    const data = try agnostic.loadTwoSumCases(allocator);
    return buildSuite(TwoSumInput, []i32, allocator, data, "two-sum", "hash-map-two-sum", twoSum, twoSumInput, intListOutput, sliceEquality);
}

pub fn validParenthesesSuite(allocator: std.mem.Allocator) !ProblemSuite([]const u8, bool) {
    const data = try agnostic.loadValidParenthesesCases(allocator);
    return buildSuite([]const u8, bool, allocator, data, "valid-parentheses", "stack-validation", validParentheses, stringInput, boolOutput, boolEquality);
}

pub fn numberOfIslandsSuite(allocator: std.mem.Allocator) !ProblemSuite([]const []const i32, i32) {
    const data = try agnostic.loadNumberOfIslandsCases(allocator);
    return buildSuite([]const []const i32, i32, allocator, data, "number-of-islands", "dfs-grid-traversal", numberOfIslands, matrixInput, intOutputMatrixInput, intEquality);
}

pub fn courseScheduleSuite(allocator: std.mem.Allocator) !ProblemSuite(CourseScheduleInput, bool) {
    const data = try agnostic.loadCourseScheduleCases(allocator);
    return buildSuite(CourseScheduleInput, bool, allocator, data, "course-schedule", "topological-sort", canFinishCourses, courseScheduleInput, boolOutputCourseInput, boolEquality);
}

pub fn trappingRainWaterSuite(allocator: std.mem.Allocator) !ProblemSuite([]const i32, i32) {
    const data = try agnostic.loadTrappingRainWaterCases(allocator);
    return buildSuite([]const i32, i32, allocator, data, "trapping-rain-water", "two-pointer-scan", trapRainWater, intListInput, intOutputListInput, intEquality);
}

pub fn runProblems(allocator: std.mem.Allocator) !bool {
    var two_sum_suite = try twoSumSuite(allocator);
    defer two_sum_suite.deinit();
    var two_sum_result = try two_sum_suite.run();
    defer two_sum_result.deinit();
    var valid_suite = try validParenthesesSuite(allocator);
    defer valid_suite.deinit();
    var valid_result = try valid_suite.run();
    defer valid_result.deinit();
    var islands_suite = try numberOfIslandsSuite(allocator);
    defer islands_suite.deinit();
    var islands_result = try islands_suite.run();
    defer islands_result.deinit();
    var courses_suite = try courseScheduleSuite(allocator);
    defer courses_suite.deinit();
    var courses_result = try courses_suite.run();
    defer courses_result.deinit();
    var rain_suite = try trappingRainWaterSuite(allocator);
    defer rain_suite.deinit();
    var rain_result = try rain_suite.run();
    defer rain_result.deinit();

    const results = [_]framework.AlgorithmSuiteResult{ two_sum_result, valid_result, islands_result, courses_result, rain_result };
    return framework.AlgorithmTestRunner.printReport(&results);
}

fn buildSuite(
    comptime Input: type,
    comptime Output: type,
    allocator: std.mem.Allocator,
    data: []agnostic.CaseData,
    suite_name: []const u8,
    algorithm_name: []const u8,
    algorithm: framework.Algorithm(Input, Output),
    input_factory: framework.InputFactory(Input),
    expected_output: framework.ExpectedOutput(Input, Output),
    equality: framework.Equality(Output),
) !ProblemSuite(Input, Output) {
    errdefer allocator.free(data);
    const cases = try allocator.alloc(framework.AlgorithmCase(Input, Output), data.len);
    errdefer allocator.free(cases);
    for (data, 0..) |*case_data, index| {
        cases[index] = .{
            .name = case_data.name,
            .input_factory = input_factory,
            .expected_output = expected_output,
            .equality = equality,
            .context = case_data,
            .budget = case_data.budget(),
        };
    }
    const adapted = agnostic.AdaptedCases(Input, Output){
        .data = data,
        .cases = cases,
        .allocator = allocator,
    };
    return .{
        .adapted = adapted,
        .suite = .{
            .suite_name = suite_name,
            .algorithm_name = algorithm_name,
            .algorithm = algorithm,
            .cases = adapted.cases,
            .options = framework.BenchmarkOptions.quick(),
            .allocator = allocator,
        },
    };
}

fn twoSumInput(allocator: std.mem.Allocator, context: ?*const anyopaque) !TwoSumInput {
    const case_data = contextData(context);
    return .{
        .nums = try agnostic.parseIntList(allocator, try recordField(case_data.input, "\"nums\":")),
        .target = try agnostic.parseInt(try recordField(case_data.input, "\"target\":")),
    };
}

fn courseScheduleInput(allocator: std.mem.Allocator, context: ?*const anyopaque) !CourseScheduleInput {
    const case_data = contextData(context);
    return .{
        .num_courses = @as(usize, @intCast(try agnostic.parseInt(try recordField(case_data.input, "\"numCourses\":")))),
        .prerequisites = try agnostic.parseIntMatrix(allocator, try recordField(case_data.input, "\"prerequisites\":")),
    };
}

fn stringInput(_: std.mem.Allocator, context: ?*const anyopaque) ![]const u8 {
    const case_data = contextData(context);
    return agnostic.parseString(case_data.input);
}

fn matrixInput(allocator: std.mem.Allocator, context: ?*const anyopaque) ![]const []const i32 {
    const case_data = contextData(context);
    return agnostic.parseIntMatrix(allocator, case_data.input);
}

fn intListInput(allocator: std.mem.Allocator, context: ?*const anyopaque) ![]const i32 {
    const case_data = contextData(context);
    return agnostic.parseIntList(allocator, case_data.input);
}

fn intListOutput(allocator: std.mem.Allocator, _: TwoSumInput, context: ?*const anyopaque) ![]i32 {
    const case_data = contextData(context);
    return agnostic.parseIntList(allocator, case_data.output);
}

fn boolOutput(_: std.mem.Allocator, _: []const u8, context: ?*const anyopaque) !bool {
    const case_data = contextData(context);
    return agnostic.parseBool(case_data.output);
}

fn boolOutputCourseInput(_: std.mem.Allocator, _: CourseScheduleInput, context: ?*const anyopaque) !bool {
    const case_data = contextData(context);
    return agnostic.parseBool(case_data.output);
}

fn intOutputMatrixInput(_: std.mem.Allocator, _: []const []const i32, context: ?*const anyopaque) !i32 {
    const case_data = contextData(context);
    return agnostic.parseInt(case_data.output);
}

fn intOutputListInput(_: std.mem.Allocator, _: []const i32, context: ?*const anyopaque) !i32 {
    const case_data = contextData(context);
    return agnostic.parseInt(case_data.output);
}

fn floodFill(grid: []const []const i32, visited: []bool, rows: usize, cols: usize, row: isize, col: isize) void {
    if (row < 0 or col < 0) return;
    const row_index = @as(usize, @intCast(row));
    const col_index = @as(usize, @intCast(col));
    if (row_index >= rows or col_index >= cols) return;
    const visited_index = row_index * cols + col_index;
    if (visited[visited_index] or grid[row_index][col_index] == 0) return;
    visited[visited_index] = true;
    floodFill(grid, visited, rows, cols, row + 1, col);
    floodFill(grid, visited, rows, cols, row - 1, col);
    floodFill(grid, visited, rows, cols, row, col + 1);
    floodFill(grid, visited, rows, cols, row, col - 1);
}

fn recordField(record: []const u8, key: []const u8) ![]const u8 {
    const key_start = std.mem.indexOf(u8, record, key) orelse return error.MissingField;
    var start = key_start + key.len;
    while (start < record.len and (record[start] == ' ' or record[start] == '\t')) start += 1;
    if (record[start] == '[') {
        const end = findMatching(record, start, '[', ']') orelse return error.InvalidRecord;
        return record[start .. end + 1];
    }
    var end = start;
    while (end < record.len and record[end] != ',' and record[end] != '}') end += 1;
    return std.mem.trim(u8, record[start..end], " \r\n\t");
}

fn findMatching(value: []const u8, start: usize, open: u8, close: u8) ?usize {
    var depth: i32 = 0;
    for (value[start..], start..) |char, index| {
        if (char == open) {
            depth += 1;
        } else if (char == close) {
            depth -= 1;
            if (depth == 0) return index;
        }
    }
    return null;
}

fn contextData(context: ?*const anyopaque) *const agnostic.CaseData {
    return @as(*const agnostic.CaseData, @ptrCast(@alignCast(context.?)));
}

fn matches(open: u8, close: u8) bool {
    return (open == '(' and close == ')') or
        (open == '[' and close == ']') or
        (open == '{' and close == '}');
}

fn sliceEquality(expected: []i32, actual: []i32) bool {
    return std.mem.eql(i32, expected, actual);
}

fn intEquality(expected: i32, actual: i32) bool {
    return expected == actual;
}

fn boolEquality(expected: bool, actual: bool) bool {
    return expected == actual;
}
