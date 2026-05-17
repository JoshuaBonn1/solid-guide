const std = @import("std");
const examples = @import("examples.zig");
const problems = @import("problems.zig");

pub fn main() !void {
    var debug_allocator = std.heap.DebugAllocator(.{}){};
    defer _ = debug_allocator.deinit();

    const passed = try examples.runExamples(debug_allocator.allocator()) and
        try problems.runProblems(debug_allocator.allocator());
    if (!passed) {
        std.process.exit(1);
    }
}
