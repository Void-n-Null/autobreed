package net.voidnull.autobreed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HayBaleCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Thread-safe map of chunks to hay bale positions
    private static final Map<ChunkPos, Set<BlockPos>> chunkToHayMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned to avoid re-scanning
    private static final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public static void onChunkLoad(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        if (scannedChunks.contains(chunkPos)) {
            return;
        }
        
        Set<BlockPos> hayPositions = new HashSet<>();
        
        // Scan the chunk for hay bales
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(
                        chunkPos.getMinBlockX() + x,
                        y,
                        chunkPos.getMinBlockZ() + z
                    );
                    if (chunk.getBlockState(pos).is(Blocks.HAY_BLOCK)) {
                        hayPositions.add(pos);
                        // Also register with the data manager if not already tracked
                        if (!HayBaleDataManager.getEatenCounts().containsKey(pos)) {
                            HayBaleDataManager.addHayBale(pos);
                        }
                    }
                }
            }
        }
        
        if (!hayPositions.isEmpty()) {
            chunkToHayMap.put(chunkPos, Collections.synchronizedSet(hayPositions));
            LOGGER.debug("Found {} hay bales in chunk {}", hayPositions.size(), chunkPos);
        }
        
        scannedChunks.add(chunkPos);
    }
    
    public static void onChunkUnload(ChunkPos chunkPos) {
        chunkToHayMap.remove(chunkPos);
        scannedChunks.remove(chunkPos);
    }
    
    public static void addHayBale(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        chunkToHayMap.computeIfAbsent(chunkPos, k -> Collections.synchronizedSet(new HashSet<>()))
                     .add(pos);
    }
    
    public static void removeHayBale(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<BlockPos> hayInChunk = chunkToHayMap.get(chunkPos);
        if (hayInChunk != null) {
            hayInChunk.remove(pos);
            if (hayInChunk.isEmpty()) {
                chunkToHayMap.remove(chunkPos);
            }
        }
    }
    
    /**
     * Quick check if there are any hay bales in the general area.
     * Much faster than findNearestHayBale as it only checks chunk existence.
     */
    public static boolean hasHayBalesNearby(BlockPos pos, int maxRadius) {
        ChunkPos centerChunk = new ChunkPos(pos);
        int chunkRadius = (maxRadius >> 4) + 1;  // Convert block radius to chunk radius

        // Quick check of surrounding chunks
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos checkChunk = new ChunkPos(
                    centerChunk.x + dx,
                    centerChunk.z + dz
                );
                
                Set<BlockPos> hayInChunk = chunkToHayMap.get(checkChunk);
                if (hayInChunk != null && !hayInChunk.isEmpty()) {
                    // Found a chunk with hay, now check if any aren't fully eaten
                    for (BlockPos hayPos : hayInChunk) {
                        if (HayBaleDataManager.getEatenCount(hayPos) < 10) {
                            return true;  // Found at least one edible hay bale!
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public static BlockPos findNearestHayBale(BlockPos start, int maxRadius) {
        // First do the quick check
        if (!hasHayBalesNearby(start, maxRadius)) {
            return null;  // No hay bales nearby, don't bother with expensive search
        }
        
        ChunkPos centerChunk = new ChunkPos(start);
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        // Search in expanding square of chunks
        for (int r = 0; r <= (maxRadius >> 4) + 1; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only check the perimeter chunks for r > 0
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    
                    ChunkPos checkChunk = new ChunkPos(
                        centerChunk.x + dx,
                        centerChunk.z + dz
                    );
                    
                    Set<BlockPos> hayInChunk = chunkToHayMap.get(checkChunk);
                    if (hayInChunk != null) {
                        for (BlockPos hayPos : hayInChunk) {
                            // Skip if fully eaten
                            if (HayBaleDataManager.getEatenCount(hayPos) >= 10) continue;
                            
                            double distSq = start.distSqr(hayPos);
                            if (distSq < nearestDistSq && distSq <= maxRadius * maxRadius) {
                                nearest = hayPos;
                                nearestDistSq = distSq;
                            }
                        }
                    }
                }
            }
            
            // If we found something in this radius, no need to search further
            if (nearest != null) break;
        }
        
        return nearest;
    }
    
    public static void clear() {
        chunkToHayMap.clear();
        scannedChunks.clear();
    }
} 