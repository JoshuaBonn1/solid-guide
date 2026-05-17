const std = @import("std");

pub fn Algorithm(comptime Input: type, comptime Output: type) type {
    return *const fn (std.mem.Allocator, Input) anyerror!Output;
}

pub fn InputFactory(comptime Input: type) type {
    return *const fn (std.mem.Allocator) anyerror!Input;
}

pub fn ExpectedOutput(comptime Input: type, comptime Output: type) type {
    return *const fn (std.mem.Allocator, Input) anyerror!Output;
}

pub fn Equality(comptime Output: type) type {
    return *const fn (Output, Output) bool;
}

pub const BenchmarkOptions = struct {
    warmup_iterations: usize = 5,
    measurement_iterations: usize = 25,
    measure_memory: bool = true,
    gc_before_memory_measurement: bool = true,

    pub fn defaults() BenchmarkOptions {
        return .{};
    }

    pub fn quick() BenchmarkOptions {
        return .{
            .warmup_iterations = 1,
            .measurement_iterations = 5,
        };
    }

    pub fn withoutMemoryMeasurements(self: BenchmarkOptions) BenchmarkOptions {
        return .{
            .warmup_iterations = self.warmup_iterations,
            .measurement_iterations = self.measurement_iterations,
            .measure_memory = false,
            .gc_before_memory_measurement = false,
        };
    }

    pub fn validate(self: BenchmarkOptions) !void {
        if (self.measurement_iterations == 0) {
            return error.MeasurementIterationsMustBePositive;
        }
    }
};

pub const ComplexityBudget = struct {
    max_average_duration_ns: ?u64 = null,
    max_memory_delta_bytes: ?usize = null,

    pub fn none() ComplexityBudget {
        return .{};
    }

    pub fn withLimits(max_average_duration_ns: ?u64, max_memory_delta_bytes: ?usize) ComplexityBudget {
        return .{
            .max_average_duration_ns = max_average_duration_ns,
            .max_memory_delta_bytes = max_memory_delta_bytes,
        };
    }
};

pub const ExecutionStats = struct {
    iterations: usize,
    total_duration_ns: u64,
    min_duration_ns: u64,
    max_duration_ns: u64,
    max_memory_delta_bytes: usize,
    total_memory_delta_bytes: usize,

    pub fn averageDurationNs(self: ExecutionStats) u64 {
        return self.total_duration_ns / self.iterations;
    }

    pub fn averageMemoryDeltaBytes(self: ExecutionStats) usize {
        return self.total_memory_delta_bytes / self.iterations;
    }
};

pub fn AlgorithmCase(comptime Input: type, comptime Output: type) type {
    return struct {
        name: []const u8,
        input_factory: InputFactory(Input),
        expected_output: ExpectedOutput(Input, Output),
        equality: Equality(Output),
        budget: ComplexityBudget = ComplexityBudget.none(),
    };
}

pub const AlgorithmCaseResult = struct {
    case_name: []const u8,
    correct: bool,
    stats: ?ExecutionStats = null,
    budget_violation_count: usize = 0,
    error_name: ?[]const u8 = null,

    pub fn passed(self: AlgorithmCaseResult) bool {
        return self.correct and self.budget_violation_count == 0 and self.error_name == null;
    }
};

pub const AlgorithmSuiteResult = struct {
    suite_name: []const u8,
    algorithm_name: []const u8,
    case_results: []AlgorithmCaseResult,
    allocator: std.mem.Allocator,

    pub fn deinit(self: AlgorithmSuiteResult) void {
        self.allocator.free(self.case_results);
    }

    pub fn passed(self: AlgorithmSuiteResult) bool {
        for (self.case_results) |case_result| {
            if (!case_result.passed()) {
                return false;
            }
        }
        return true;
    }

    pub fn passedCaseCount(self: AlgorithmSuiteResult) usize {
        var count: usize = 0;
        for (self.case_results) |case_result| {
            if (case_result.passed()) {
                count += 1;
            }
        }
        return count;
    }

    pub fn failedCaseCount(self: AlgorithmSuiteResult) usize {
        return self.case_results.len - self.passedCaseCount();
    }
};

pub fn AlgorithmTestSuite(comptime Input: type, comptime Output: type) type {
    return struct {
        const Self = @This();
        const Case = AlgorithmCase(Input, Output);

        suite_name: []const u8,
        algorithm_name: []const u8,
        algorithm: Algorithm(Input, Output),
        cases: []const Case,
        options: BenchmarkOptions = BenchmarkOptions.defaults(),
        allocator: std.mem.Allocator,

        pub fn run(self: Self) !AlgorithmSuiteResult {
            if (self.suite_name.len == 0 or self.algorithm_name.len == 0 or self.cases.len == 0) {
                return error.InvalidSuite;
            }
            try self.options.validate();

            const results = try self.allocator.alloc(AlgorithmCaseResult, self.cases.len);
            for (self.cases, 0..) |test_case, index| {
                results[index] = self.runCase(test_case);
            }

            return .{
                .suite_name = self.suite_name,
                .algorithm_name = self.algorithm_name,
                .case_results = results,
                .allocator = self.allocator,
            };
        }

        fn runCase(self: Self, test_case: Case) AlgorithmCaseResult {
            var arena = std.heap.ArenaAllocator.init(self.allocator);
            defer arena.deinit();
            const allocator = arena.allocator();

            const expected_input = test_case.input_factory(allocator) catch |err| {
                return failedCase(test_case.name, err);
            };
            const expected = test_case.expected_output(allocator, expected_input) catch |err| {
                return failedCase(test_case.name, err);
            };
            const actual_input = test_case.input_factory(allocator) catch |err| {
                return failedCase(test_case.name, err);
            };
            const actual = self.algorithm(allocator, actual_input) catch |err| {
                return failedCase(test_case.name, err);
            };

            if (!test_case.equality(expected, actual)) {
                return .{
                    .case_name = test_case.name,
                    .correct = false,
                };
            }

            const stats = self.measure(test_case) catch |err| {
                return failedCase(test_case.name, err);
            };

            return .{
                .case_name = test_case.name,
                .correct = true,
                .stats = stats,
                .budget_violation_count = budgetViolationCount(test_case.budget, stats),
            };
        }

        fn measure(self: Self, test_case: Case) !ExecutionStats {
            for (0..self.options.warmup_iterations) |_| {
                var arena = std.heap.ArenaAllocator.init(self.allocator);
                defer arena.deinit();
                const allocator = arena.allocator();
                const input = try test_case.input_factory(allocator);
                const output = try self.algorithm(allocator, input);
                std.mem.doNotOptimizeAway(output);
            }

            var total_duration_ns: u64 = 0;
            var min_duration_ns: u64 = std.math.maxInt(u64);
            var max_duration_ns: u64 = 0;
            var total_memory_delta_bytes: usize = 0;
            var max_memory_delta_bytes: usize = 0;

            for (0..self.options.measurement_iterations) |_| {
                var arena = std.heap.ArenaAllocator.init(self.allocator);
                defer arena.deinit();
                const allocator = arena.allocator();

                const memory_before = if (self.options.measure_memory) arena.queryCapacity() else 0;
                const input = try test_case.input_factory(allocator);
                const started = std.time.nanoTimestamp();
                const output = try self.algorithm(allocator, input);
                const elapsed_ns = @as(u64, @intCast(std.time.nanoTimestamp() - started));
                std.mem.doNotOptimizeAway(output);
                const memory_after = if (self.options.measure_memory) arena.queryCapacity() else memory_before;
                const memory_delta = if (memory_after > memory_before) memory_after - memory_before else 0;

                total_duration_ns += elapsed_ns;
                min_duration_ns = @min(min_duration_ns, elapsed_ns);
                max_duration_ns = @max(max_duration_ns, elapsed_ns);
                total_memory_delta_bytes += memory_delta;
                max_memory_delta_bytes = @max(max_memory_delta_bytes, memory_delta);
            }

            return .{
                .iterations = self.options.measurement_iterations,
                .total_duration_ns = total_duration_ns,
                .min_duration_ns = min_duration_ns,
                .max_duration_ns = max_duration_ns,
                .max_memory_delta_bytes = max_memory_delta_bytes,
                .total_memory_delta_bytes = total_memory_delta_bytes,
            };
        }
    };
}

pub const AlgorithmTestRunner = struct {
    pub fn printReport(writer: anytype, results: []const AlgorithmSuiteResult) !bool {
        var all_passed = true;
        for (results) |result| {
            all_passed = all_passed and result.passed();
            try writeReport(writer, result);
            try writer.print("\n", .{});
        }
        return all_passed;
    }

    pub fn writeReport(writer: anytype, result: AlgorithmSuiteResult) !void {
        try writer.print(
            "Suite: {s} | Algorithm: {s} | {d}/{d} passed\n",
            .{ result.suite_name, result.algorithm_name, result.passedCaseCount(), result.case_results.len },
        );

        for (result.case_results) |case_result| {
            try writer.print("  [{s}] {s}", .{ if (case_result.passed()) "PASS" else "FAIL", case_result.case_name });
            if (case_result.stats) |stats| {
                try writer.print(
                    " | avg {d}ns | min {d}ns | max {d}ns | max memory delta {d} bytes",
                    .{ stats.averageDurationNs(), stats.min_duration_ns, stats.max_duration_ns, stats.max_memory_delta_bytes },
                );
            }
            if (!case_result.correct) {
                try writer.print(" | correctness failed", .{});
            }
            if (case_result.budget_violation_count > 0) {
                try writer.print(" | budget violations {d}", .{case_result.budget_violation_count});
            }
            if (case_result.error_name) |error_name| {
                try writer.print(" | error={s}", .{error_name});
            }
            try writer.print("\n", .{});
        }
    }

    pub fn allPassed(results: []const AlgorithmSuiteResult) bool {
        for (results) |result| {
            if (!result.passed()) {
                return false;
            }
        }
        return true;
    }

};

fn failedCase(case_name: []const u8, err: anyerror) AlgorithmCaseResult {
    return .{
        .case_name = case_name,
        .correct = false,
        .error_name = @errorName(err),
    };
}

fn budgetViolationCount(budget: ComplexityBudget, stats: ExecutionStats) usize {
    var count: usize = 0;
    if (budget.max_average_duration_ns) |max_duration_ns| {
        if (stats.averageDurationNs() > max_duration_ns) {
            count += 1;
        }
    }
    if (budget.max_memory_delta_bytes) |max_memory_delta_bytes| {
        if (stats.max_memory_delta_bytes > max_memory_delta_bytes) {
            count += 1;
        }
    }
    return count;
}
