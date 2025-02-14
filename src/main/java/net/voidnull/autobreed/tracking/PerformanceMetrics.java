package net.voidnull.autobreed.tracking;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.DoubleSummaryStatistics;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceMetrics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private static final Map<String, List<Long>> durations = new ConcurrentHashMap<>();
    
    public static void startTimer(String operation) {
        startTimes.put(operation, System.nanoTime());
    }
    
    public static void stopTimer(String operation) {
        Long start = startTimes.remove(operation);
        if (start != null) {
            long duration = System.nanoTime() - start;
            durations.computeIfAbsent(operation, k -> new ArrayList<>()).add(duration);
        }
    }
    
    public static void logStats() {
        durations.forEach((operation, times) -> {
            DoubleSummaryStatistics stats = times.stream()
                .mapToDouble(t -> t / 1_000_000.0) // Convert to milliseconds
                .summaryStatistics();
            
            LOGGER.info("{} stats (ms):", operation);
            LOGGER.info("  Count: {}", stats.getCount());
            LOGGER.info("  Avg: {}", String.format("%.2f", stats.getAverage()));
            LOGGER.info("  Min: {}", String.format("%.2f", stats.getMin()));
            LOGGER.info("  Max: {}", String.format("%.2f", stats.getMax()));
        });
        
        // Clear the stats after logging
        durations.clear();
    }
} 