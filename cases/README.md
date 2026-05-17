# Shared algorithm cases

Algorithm cases are defined once in this directory and adapted by each language
implementation.

The current format is tab-separated, dependency-free, and intentionally small:

```text
# solid-guide-cases-v1
# suite: sorting
# input: list<int>
# output: list<int>
name	input	output	max_average_duration_ns	max_memory_delta_bytes
empty input	[]	[]	5000000	1048576
```

Rules:

- Lines beginning with `#` are metadata or comments.
- The first non-comment line is the header.
- `input` and `output` cells use a JSON-like value grammar:
  - integers: `42`
  - booleans: `true`, `false`
  - strings: `"label"`
  - lists: `[1,2,3]`
  - matrices / nested lists: `[[1,2],[3,4]]`
  - records: `{"values":[1,3,5],"target":3}`
- Budgets use fixed units across languages: nanoseconds and bytes.

Adapters are responsible for converting neutral values into language-native
types. For example, `list<int>` becomes `List<Integer>` in Java, `list[int]` in
Python, and `[]i32` / `[]const i32` in Zig. In-place algorithms should adapt by
copying the neutral input into a fresh mutable structure, running the mutation,
and returning the mutated structure as the comparable output.

The value grammar is deliberately compatible with common data structures:
scalars, sequences, matrices, records, and graph-like records such as
`{"nodes":4,"edges":[[0,1],[1,2]]}`. Add fields or new files as algorithms need
more shape; keep language-specific construction in adapter code rather than in
these shared case files.

Current problem fixtures include:

- `sorting.tsv` and `search.tsv`: baseline examples used by every language.
- `two_sum.tsv` (easy): hash-map lookup over an integer list.
- `valid_parentheses.tsv` (easy): stack validation over a string.
- `number_of_islands.tsv` (medium): grid traversal over a matrix.
- `course_schedule.tsv` (medium): graph cycle detection / topological ordering.
- `trapping_rain_water.tsv` (hard): two-pointer scan over an elevation list.
