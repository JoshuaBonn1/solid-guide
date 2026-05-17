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

    public record AnagramInput(String s, String t) {
    }

    public record EditDistanceInput(String word1, String word2) {
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

    public static int maxProfit(List<Integer> prices) {
        int minPrice = Integer.MAX_VALUE;
        int bestProfit = 0;
        for (int price : prices) {
            minPrice = Math.min(minPrice, price);
            bestProfit = Math.max(bestProfit, price - minPrice);
        }
        return bestProfit;
    }

    public static boolean validAnagram(AnagramInput input) {
        if (input.s().length() != input.t().length()) {
            return false;
        }
        int[] counts = new int[26];
        for (int i = 0; i < input.s().length(); i++) {
            counts[input.s().charAt(i) - 'a']++;
            counts[input.t().charAt(i) - 'a']--;
        }
        for (int count : counts) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }

    public static int maximumSubarray(List<Integer> nums) {
        int current = nums.get(0);
        int best = nums.get(0);
        for (int i = 1; i < nums.size(); i++) {
            current = Math.max(nums.get(i), current + nums.get(i));
            best = Math.max(best, current);
        }
        return best;
    }

    public static List<List<Integer>> mergeIntervals(List<List<Integer>> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<List<Integer>> sorted = new ArrayList<>(intervals);
        sorted.sort(java.util.Comparator.comparingInt(interval -> interval.get(0)));
        List<List<Integer>> merged = new ArrayList<>();
        int start = sorted.get(0).get(0);
        int end = sorted.get(0).get(1);
        for (int i = 1; i < sorted.size(); i++) {
            int nextStart = sorted.get(i).get(0);
            int nextEnd = sorted.get(i).get(1);
            if (nextStart <= end) {
                end = Math.max(end, nextEnd);
            } else {
                merged.add(List.of(start, end));
                start = nextStart;
                end = nextEnd;
            }
        }
        merged.add(List.of(start, end));
        return merged;
    }

    public static int editDistance(EditDistanceInput input) {
        int rows = input.word1().length();
        int cols = input.word2().length();
        int[][] dp = new int[rows + 1][cols + 1];
        for (int row = 0; row <= rows; row++) {
            dp[row][0] = row;
        }
        for (int col = 0; col <= cols; col++) {
            dp[0][col] = col;
        }
        for (int row = 1; row <= rows; row++) {
            for (int col = 1; col <= cols; col++) {
                if (input.word1().charAt(row - 1) == input.word2().charAt(col - 1)) {
                    dp[row][col] = dp[row - 1][col - 1];
                } else {
                    dp[row][col] = 1 + Math.min(
                            dp[row - 1][col - 1],
                            Math.min(dp[row - 1][col], dp[row][col - 1]));
                }
            }
        }
        return dp[rows][cols];
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
