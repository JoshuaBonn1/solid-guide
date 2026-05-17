package com.solidguide.algorithms.examples;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CodingProblems {
    private CodingProblems() {
    }

    public record TwoSumInput(List<Integer> nums, int target) {
    }

    public record CourseScheduleInput(int numCourses, List<List<Integer>> prerequisites) {
    }

    public static List<Integer> twoSum(TwoSumInput input) {
        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < input.nums().size(); i++) {
            int value = input.nums().get(i);
            int complement = input.target() - value;
            if (seen.containsKey(complement)) {
                return List.of(seen.get(complement), i);
            }
            seen.put(value, i);
        }
        throw new IllegalArgumentException("no two-sum solution");
    }

    public static boolean validParentheses(String input) {
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (current == '(' || current == '[' || current == '{') {
                stack.push(current);
            } else if (stack.isEmpty() || !matches(stack.pop(), current)) {
                return false;
            }
        }
        return stack.isEmpty();
    }

    public static int numberOfIslands(List<List<Integer>> grid) {
        if (grid.isEmpty() || grid.get(0).isEmpty()) {
            return 0;
        }
        boolean[][] visited = new boolean[grid.size()][grid.get(0).size()];
        int islands = 0;
        for (int row = 0; row < grid.size(); row++) {
            for (int col = 0; col < grid.get(row).size(); col++) {
                if (grid.get(row).get(col) == 1 && !visited[row][col]) {
                    islands++;
                    floodFill(grid, visited, row, col);
                }
            }
        }
        return islands;
    }

    public static boolean canFinishCourses(CourseScheduleInput input) {
        List<List<Integer>> graph = new ArrayList<>(input.numCourses());
        for (int i = 0; i < input.numCourses(); i++) {
            graph.add(new ArrayList<>());
        }
        int[] indegree = new int[input.numCourses()];
        for (List<Integer> prerequisite : input.prerequisites()) {
            int course = prerequisite.get(0);
            int required = prerequisite.get(1);
            graph.get(required).add(course);
            indegree[course]++;
        }

        Deque<Integer> ready = new ArrayDeque<>();
        for (int i = 0; i < indegree.length; i++) {
            if (indegree[i] == 0) {
                ready.add(i);
            }
        }

        int completed = 0;
        while (!ready.isEmpty()) {
            int course = ready.remove();
            completed++;
            for (int next : graph.get(course)) {
                indegree[next]--;
                if (indegree[next] == 0) {
                    ready.add(next);
                }
            }
        }
        return completed == input.numCourses();
    }

    public static int trapRainWater(List<Integer> heights) {
        int left = 0;
        int right = heights.size() - 1;
        int leftMax = 0;
        int rightMax = 0;
        int water = 0;
        while (left < right) {
            if (heights.get(left) < heights.get(right)) {
                leftMax = Math.max(leftMax, heights.get(left));
                water += leftMax - heights.get(left);
                left++;
            } else {
                rightMax = Math.max(rightMax, heights.get(right));
                water += rightMax - heights.get(right);
                right--;
            }
        }
        return water;
    }

    private static void floodFill(List<List<Integer>> grid, boolean[][] visited, int row, int col) {
        if (row < 0 || col < 0 || row >= grid.size() || col >= grid.get(row).size()) {
            return;
        }
        if (visited[row][col] || grid.get(row).get(col) == 0) {
            return;
        }
        visited[row][col] = true;
        floodFill(grid, visited, row + 1, col);
        floodFill(grid, visited, row - 1, col);
        floodFill(grid, visited, row, col + 1);
        floodFill(grid, visited, row, col - 1);
    }

    private static boolean matches(char open, char close) {
        return (open == '(' && close == ')')
                || (open == '[' && close == ']')
                || (open == '{' && close == '}');
    }
}
