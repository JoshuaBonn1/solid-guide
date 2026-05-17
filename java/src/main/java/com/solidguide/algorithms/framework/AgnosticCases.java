package com.solidguide.algorithms.framework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AgnosticCases {
    private AgnosticCases() {
    }

    public static List<AgnosticCase> load(String fileName) {
        Path path = resolveCaseFile(fileName);
        List<AgnosticCase> cases = new ArrayList<>();
        boolean headerSeen = false;
        try {
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (!headerSeen) {
                    headerSeen = true;
                    continue;
                }
                String[] columns = rawLine.split("\t", -1);
                if (columns.length != 5) {
                    throw new IllegalArgumentException("expected 5 tab-separated columns in " + rawLine);
                }
                cases.add(new AgnosticCase(
                        columns[0],
                        columns[1],
                        columns[2],
                        parseOptionalLong(columns[3]),
                        parseOptionalLong(columns[4])));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read " + path, exception);
        }
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("no cases found in " + path);
        }
        return List.copyOf(cases);
    }

    public static <I, O> List<AlgorithmCase<I, O>> adaptOutOfPlace(
            List<AgnosticCase> cases,
            Function<String, I> inputAdapter,
            Function<String, O> outputAdapter) {
        List<AlgorithmCase<I, O>> adapted = new ArrayList<>(cases.size());
        for (AgnosticCase testCase : cases) {
            adapted.add(AlgorithmCase.<I, O>builder(testCase.name())
                    .input(() -> inputAdapter.apply(testCase.inputValue()))
                    .expect(_input -> outputAdapter.apply(testCase.outputValue()))
                    .budget(budget(testCase))
                    .build());
        }
        return List.copyOf(adapted);
    }

    public static Algorithm<List<Integer>, List<Integer>> inPlaceListAlgorithm(Consumer<List<Integer>> mutator) {
        return input -> {
            mutator.accept(input);
            return input;
        };
    }

    public static List<Integer> parseIntList(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("expected list value: " + value);
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        String[] parts = body.split(",");
        List<Integer> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            values.add(Integer.parseInt(part.trim()));
        }
        return values;
    }

    public static int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    public static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value.trim());
    }

    public static String parseString(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("\"") || !trimmed.endsWith("\"")) {
            throw new IllegalArgumentException("expected string value: " + value);
        }
        return trimmed.substring(1, trimmed.length() - 1);
    }

    public static List<List<Integer>> parseIntMatrix(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("expected matrix value: " + value);
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }
        List<List<Integer>> rows = new ArrayList<>();
        for (String row : splitTopLevel(body)) {
            rows.add(parseIntList(row));
        }
        return List.copyOf(rows);
    }

    public static Map<String, String> parseFlatRecord(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("expected record value: " + value);
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        List<String> fields = splitTopLevel(body);
        java.util.LinkedHashMap<String, String> record = new java.util.LinkedHashMap<>();
        for (String field : fields) {
            int separator = field.indexOf(':');
            if (separator < 1) {
                throw new IllegalArgumentException("expected record field: " + field);
            }
            String key = field.substring(0, separator).trim();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            record.put(key, field.substring(separator + 1).trim());
        }
        return Map.copyOf(record);
    }

    private static List<String> splitTopLevel(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '[' || current == '{') {
                depth++;
            } else if (current == ']' || current == '}') {
                depth--;
            } else if (current == ',' && depth == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private static ComplexityBudget budget(AgnosticCase testCase) {
        ComplexityBudget.Builder builder = ComplexityBudget.builder();
        if (testCase.maxAverageDurationNanos() != null) {
            builder.maxAverageDuration(Duration.ofNanos(testCase.maxAverageDurationNanos()));
        }
        if (testCase.maxMemoryDeltaBytes() != null) {
            builder.maxMemoryDeltaBytes(testCase.maxMemoryDeltaBytes());
        }
        return builder.build();
    }

    private static Long parseOptionalLong(String value) {
        return value.isBlank() ? null : Long.parseLong(value);
    }

    private static Path resolveCaseFile(String fileName) {
        Path path = Path.of(fileName);
        if (Files.exists(path)) {
            return path;
        }
        Path rootPath = Path.of("cases", fileName);
        if (Files.exists(rootPath)) {
            return rootPath;
        }
        throw new IllegalArgumentException("case file not found: " + fileName);
    }
}
