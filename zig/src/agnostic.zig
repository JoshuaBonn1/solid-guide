const std = @import("std");
const framework = @import("framework.zig");

pub const CaseData = struct {
    name: []const u8,
    input: []const u8,
    output: []const u8,
    max_average_duration_ns: ?u64,
    max_memory_delta_bytes: ?usize,

    pub fn budget(self: CaseData) framework.ComplexityBudget {
        return framework.ComplexityBudget.withLimits(
            self.max_average_duration_ns,
            self.max_memory_delta_bytes,
        );
    }
};

pub fn AdaptedCases(comptime Input: type, comptime Output: type) type {
    return struct {
        data: []CaseData,
        cases: []framework.AlgorithmCase(Input, Output),
        allocator: std.mem.Allocator,

        pub fn deinit(self: @This()) void {
            self.allocator.free(self.cases);
            self.allocator.free(self.data);
        }
    };
}

pub fn loadSortingCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("sorting_cases"));
}

pub fn loadSearchCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("search_cases"));
}

pub fn loadTwoSumCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("two_sum_cases"));
}

pub fn loadValidParenthesesCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("valid_parentheses_cases"));
}

pub fn loadNumberOfIslandsCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("number_of_islands_cases"));
}

pub fn loadCourseScheduleCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("course_schedule_cases"));
}

pub fn loadTrappingRainWaterCases(allocator: std.mem.Allocator) ![]CaseData {
    return parseCases(allocator, @embedFile("trapping_rain_water_cases"));
}

pub fn adaptSortingOutOfPlace(
    allocator: std.mem.Allocator,
    data: []CaseData,
) ![]framework.AlgorithmCase([]const i32, []i32) {
    const cases = try allocator.alloc(framework.AlgorithmCase([]const i32, []i32), data.len);
    for (data, 0..) |*case_data, index| {
        cases[index] = .{
            .name = case_data.name,
            .input_factory = intListInputConst,
            .expected_output = intListOutputConstInput,
            .equality = sliceEquality,
            .context = case_data,
            .budget = case_data.budget(),
        };
    }
    return cases;
}

pub fn adaptSortingInPlace(
    allocator: std.mem.Allocator,
    data: []CaseData,
) ![]framework.AlgorithmCase([]i32, []i32) {
    const cases = try allocator.alloc(framework.AlgorithmCase([]i32, []i32), data.len);
    for (data, 0..) |*case_data, index| {
        cases[index] = .{
            .name = case_data.name,
            .input_factory = intListInput,
            .expected_output = intListOutputMutableInput,
            .equality = sliceEquality,
            .context = case_data,
            .budget = case_data.budget(),
        };
    }
    return cases;
}

pub fn adaptSearch(
    allocator: std.mem.Allocator,
    data: []CaseData,
) ![]framework.AlgorithmCase(SearchRecord, i32) {
    const cases = try allocator.alloc(framework.AlgorithmCase(SearchRecord, i32), data.len);
    for (data, 0..) |*case_data, index| {
        cases[index] = .{
            .name = case_data.name,
            .input_factory = searchInput,
            .expected_output = intOutput,
            .equality = intEquality,
            .context = case_data,
            .budget = case_data.budget(),
        };
    }
    return cases;
}

pub const SearchRecord = struct {
    values: []const i32,
    target: i32,
};

pub fn parseIntList(allocator: std.mem.Allocator, value: []const u8) ![]i32 {
    const trimmed = std.mem.trim(u8, value, " \r\n\t");
    if (trimmed.len < 2 or trimmed[0] != '[' or trimmed[trimmed.len - 1] != ']') {
        return error.ExpectedList;
    }
    const body = std.mem.trim(u8, trimmed[1 .. trimmed.len - 1], " \r\n\t");
    if (body.len == 0) {
        return allocator.alloc(i32, 0);
    }

    var count: usize = 1;
    for (body) |char| {
        if (char == ',') count += 1;
    }

    const values = try allocator.alloc(i32, count);
    var parts = std.mem.splitScalar(u8, body, ',');
    var index: usize = 0;
    while (parts.next()) |part| : (index += 1) {
        values[index] = try parseInt(part);
    }
    return values;
}

pub fn parseInt(value: []const u8) !i32 {
    return std.fmt.parseInt(i32, std.mem.trim(u8, value, " \r\n\t"), 10);
}

pub fn parseBool(value: []const u8) !bool {
    const trimmed = std.mem.trim(u8, value, " \r\n\t");
    if (std.mem.eql(u8, trimmed, "true")) return true;
    if (std.mem.eql(u8, trimmed, "false")) return false;
    return error.ExpectedBool;
}

pub fn parseString(value: []const u8) ![]const u8 {
    const trimmed = std.mem.trim(u8, value, " \r\n\t");
    if (trimmed.len < 2 or trimmed[0] != '"' or trimmed[trimmed.len - 1] != '"') {
        return error.ExpectedString;
    }
    return trimmed[1 .. trimmed.len - 1];
}

pub fn parseIntMatrix(allocator: std.mem.Allocator, value: []const u8) ![]const []const i32 {
    const trimmed = std.mem.trim(u8, value, " \r\n\t");
    if (trimmed.len < 2 or trimmed[0] != '[' or trimmed[trimmed.len - 1] != ']') {
        return error.ExpectedMatrix;
    }
    const body = std.mem.trim(u8, trimmed[1 .. trimmed.len - 1], " \r\n\t");
    if (body.len == 0) {
        return allocator.alloc([]const i32, 0);
    }

    const row_count = countTopLevelParts(body);
    const rows = try allocator.alloc([]const i32, row_count);
    var start: usize = 0;
    var depth: i32 = 0;
    var index: usize = 0;
    for (body, 0..) |char, position| {
        if (char == '[' or char == '{') {
            depth += 1;
        } else if (char == ']' or char == '}') {
            depth -= 1;
        } else if (char == ',' and depth == 0) {
            rows[index] = try parseIntList(allocator, body[start..position]);
            index += 1;
            start = position + 1;
        }
    }
    rows[index] = try parseIntList(allocator, body[start..]);
    return rows;
}

fn parseCases(allocator: std.mem.Allocator, data: []const u8) ![]CaseData {
    var count: usize = 0;
    var lines_for_count = std.mem.splitScalar(u8, data, '\n');
    var header_seen = false;
    while (lines_for_count.next()) |raw_line| {
        const line = std.mem.trim(u8, raw_line, " \r\n\t");
        if (line.len == 0 or line[0] == '#') continue;
        if (!header_seen) {
            header_seen = true;
            continue;
        }
        count += 1;
    }
    if (count == 0) return error.NoCases;

    const cases = try allocator.alloc(CaseData, count);
    var lines = std.mem.splitScalar(u8, data, '\n');
    header_seen = false;
    var index: usize = 0;
    while (lines.next()) |raw_line| {
        const line = std.mem.trim(u8, raw_line, " \r\n");
        if (line.len == 0 or line[0] == '#') continue;
        if (!header_seen) {
            header_seen = true;
            continue;
        }
        var columns = std.mem.splitScalar(u8, line, '\t');
        cases[index] = .{
            .name = columns.next() orelse return error.InvalidCaseLine,
            .input = columns.next() orelse return error.InvalidCaseLine,
            .output = columns.next() orelse return error.InvalidCaseLine,
            .max_average_duration_ns = try optionalU64(columns.next() orelse return error.InvalidCaseLine),
            .max_memory_delta_bytes = try optionalUsize(columns.next() orelse return error.InvalidCaseLine),
        };
        index += 1;
    }
    return cases;
}

fn countTopLevelParts(value: []const u8) usize {
    var count: usize = 1;
    var depth: i32 = 0;
    for (value) |char| {
        if (char == '[' or char == '{') {
            depth += 1;
        } else if (char == ']' or char == '}') {
            depth -= 1;
        } else if (char == ',' and depth == 0) {
            count += 1;
        }
    }
    return count;
}

fn intListInput(allocator: std.mem.Allocator, context: ?*const anyopaque) ![]i32 {
    const case_data = contextData(context);
    return parseIntList(allocator, case_data.input);
}

fn intListInputConst(allocator: std.mem.Allocator, context: ?*const anyopaque) ![]const i32 {
    const case_data = contextData(context);
    return parseIntList(allocator, case_data.input);
}

fn intListOutputConstInput(allocator: std.mem.Allocator, _: []const i32, context: ?*const anyopaque) ![]i32 {
    const case_data = contextData(context);
    return parseIntList(allocator, case_data.output);
}

fn intListOutputMutableInput(allocator: std.mem.Allocator, _: []i32, context: ?*const anyopaque) ![]i32 {
    const case_data = contextData(context);
    return parseIntList(allocator, case_data.output);
}

fn searchInput(allocator: std.mem.Allocator, context: ?*const anyopaque) !SearchRecord {
    const case_data = contextData(context);
    return parseSearchRecord(allocator, case_data.input);
}

fn intOutput(_: std.mem.Allocator, _: SearchRecord, context: ?*const anyopaque) !i32 {
    const case_data = contextData(context);
    return parseInt(case_data.output);
}

fn parseSearchRecord(allocator: std.mem.Allocator, value: []const u8) !SearchRecord {
    const values_key = "\"values\":";
    const target_key = "\"target\":";
    const values_start = std.mem.indexOf(u8, value, values_key) orelse return error.MissingValues;
    const list_start = values_start + values_key.len;
    const list_end_relative = std.mem.indexOfScalar(u8, value[list_start..], ']') orelse return error.MissingValues;
    const list_text = value[list_start .. list_start + list_end_relative + 1];
    const target_start = std.mem.indexOf(u8, value, target_key) orelse return error.MissingTarget;
    const target_text = std.mem.trim(u8, value[target_start + target_key.len .. value.len - 1], " \r\n\t");
    return .{
        .values = try parseIntList(allocator, list_text),
        .target = try parseInt(target_text),
    };
}

fn contextData(context: ?*const anyopaque) *const CaseData {
    return @as(*const CaseData, @ptrCast(@alignCast(context.?)));
}

fn optionalU64(value: []const u8) !?u64 {
    const trimmed = std.mem.trim(u8, value, " \r\n\t");
    return if (trimmed.len == 0) null else try std.fmt.parseInt(u64, trimmed, 10);
}

fn optionalUsize(value: []const u8) !?usize {
    const parsed = try optionalU64(value);
    return if (parsed) |number| @as(usize, @intCast(number)) else null;
}

fn sliceEquality(expected: []i32, actual: []i32) bool {
    return std.mem.eql(i32, expected, actual);
}

fn intEquality(expected: i32, actual: i32) bool {
    return expected == actual;
}
