package net.voidnull.autobreed;

import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import net.minecraft.server.level.ServerLevel; // Import ServerLevel
import net.minecraft.world.level.Level;

public class HayBaleDataManager {

    // Use a ConcurrentHashMap for thread safety.
    private static final Map<BlockPos, Integer> eatenCounts = Collections.synchronizedMap(new HashMap<>());

    public static void addHayBale(BlockPos pos) {
        eatenCounts.put(pos, 0); // Initialize with 0 when placed
    }

    public static void removeHayBale(BlockPos pos) {
        eatenCounts.remove(pos);
    }

    public static int getEatenCount(BlockPos pos) {
        return eatenCounts.getOrDefault(pos, 0); // Return 0 if not found
    }

    public static void decrementEatenCount(BlockPos pos, Level level) { // Add Level parameter
        if (eatenCounts.containsKey(pos)) {
            int count = eatenCounts.get(pos);
             if (count < 10) {
                eatenCounts.put(pos, count + 1);
            } else {
                eatenCounts.remove(pos); // Remove from map *before* removing the block
                // Only remove the block on the server side.
                if (level instanceof ServerLevel) {  // Check for ServerLevel
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    public static Map<BlockPos, Integer> getEatenCounts() {
        return eatenCounts; // For saving/loading
    }
}