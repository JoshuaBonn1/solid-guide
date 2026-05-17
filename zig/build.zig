const std = @import("std");

pub fn build(b: *std.Build) void {
    const target = b.standardTargetOptions(.{});
    const optimize = b.standardOptimizeOption(.{});

    const exe_module = b.createModule(.{
        .root_source_file = b.path("src/main.zig"),
        .target = target,
        .optimize = optimize,
    });
    addCaseFiles(b, exe_module);
    const exe = b.addExecutable(.{
        .name = "solid-guide-zig",
        .root_module = exe_module,
    });
    b.installArtifact(exe);

    const run_cmd = b.addRunArtifact(exe);
    if (b.args) |args| {
        run_cmd.addArgs(args);
    }

    const run_step = b.step("run", "Run example benchmark report");
    run_step.dependOn(&run_cmd.step);

    const test_module = b.createModule(.{
        .root_source_file = b.path("src/tests.zig"),
        .target = target,
        .optimize = optimize,
    });
    addCaseFiles(b, test_module);
    const tests = b.addTest(.{
        .root_module = test_module,
    });

    const run_tests = b.addRunArtifact(tests);
    const test_step = b.step("test", "Run unit tests");
    test_step.dependOn(&run_tests.step);
}

fn addCaseFiles(b: *std.Build, module: *std.Build.Module) void {
    module.addAnonymousImport("sorting_cases", .{
        .root_source_file = b.path("../cases/sorting.tsv"),
    });
    module.addAnonymousImport("search_cases", .{
        .root_source_file = b.path("../cases/search.tsv"),
    });
}
