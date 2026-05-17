const std = @import("std");
const examples = @import("examples.zig");

pub fn main() !void {
    var gpa = std.heap.GeneralPurposeAllocator(.{}){};
    defer _ = gpa.deinit();

    const stdout = std.io.getStdOut().writer();
    const passed = try examples.runExamples(gpa.allocator(), stdout);
    if (!passed) {
        std.process.exit(1);
    }
}
