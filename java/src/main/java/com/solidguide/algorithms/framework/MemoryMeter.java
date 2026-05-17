package com.solidguide.algorithms.framework;

final class MemoryMeter {
    private MemoryMeter() {
    }

    static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    static void requestGcPause() {
        System.gc();
        try {
            Thread.sleep(20L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
