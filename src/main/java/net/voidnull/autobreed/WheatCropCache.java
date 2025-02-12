package net.voidnull.autobreed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WheatCropCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Thread-safe map of chunks to wheat crop positions
    private static final Map<ChunkPos, Set<BlockPos>> chunkToWheatMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned
    private static final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    public static void onChunkLoad(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        if (scannedChunks.contains(chunkPos)) {
            return;
        }
        
        Set<BlockPos> wheatPositions = new HashSet<>();
        
        // Scan the chunk for wheat crops
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
                    if (chunk.getBlockState(pos).is(Blocks.WHEAT)) {
                        wheatPositions.add(pos);
                        // Register with data manager if not already tracked
                        if (!WheatCropDataManager.getWheatCrops().containsKey(pos)) {
                            WheatCropDataManager.addWheatCrop(pos);
                            WheatCropDataManager.updateGrowthState(pos, chunk.getLevel());
                        }
                    }
                }
            }
        }
        
        if (!wheatPositions.isEmpty()) {
            chunkToWheatMap.put(chunkPos, Collections.synchronizedSet(wheatPositions));
            LOGGER.debug("Found {} wheat crops in chunk {}", wheatPositions.size(), chunkPos);
        }
        
        scannedChunks.add(chunkPos);
    }
    
    public static void onChunkUnload(ChunkPos chunkPos) {
        chunkToWheatMap.remove(chunkPos);
        scannedChunks.remove(chunkPos);
    }
    
    public static void addWheatCrop(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        chunkToWheatMap.computeIfAbsent(chunkPos, k -> Collections.synchronizedSet(new HashSet<>()))
                     .add(pos);
    }
    
    public static void removeWheatCrop(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Set<BlockPos> wheatInChunk = chunkToWheatMap.get(chunkPos);
        if (wheatInChunk != null) {
            wheatInChunk.remove(pos);
            if (wheatInChunk.isEmpty()) {
                chunkToWheatMap.remove(chunkPos);
            }
        }
    }

    /**
     * Get all wheat crops within the specified radius, regardless of growth state
     */
    public static List<BlockPos> getWheatCropsInRadius(BlockPos center, int maxRadius) {
        ChunkPos centerChunk = new ChunkPos(center);
        int chunkRadius = (maxRadius >> 4) + 1;  // Convert block radius to chunk radius
        List<BlockPos> nearbyWheat = new ArrayList<>();
        int maxRadiusSq = maxRadius * maxRadius;

        // Check all chunks in range
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos checkChunk = new ChunkPos(
                    centerChunk.x + dx,
                    centerChunk.z + dz
                );
                
                Set<BlockPos> wheatInChunk = chunkToWheatMap.get(checkChunk);
                if (wheatInChunk != null && !wheatInChunk.isEmpty()) {
                    // Add all wheat within radius
                    wheatInChunk.stream()
                        .filter(pos -> center.distSqr(pos) <= maxRadiusSq)
                        .forEach(nearbyWheat::add);
                }
            }
        }

        return nearbyWheat;
    }

    /**
     * Get all fully grown wheat crops within the specified radius
     */
    public static List<BlockPos> getFullyGrownWheatInRadius(BlockPos center, int maxRadius) {
        return getWheatCropsInRadius(center, maxRadius).stream()
            .filter(WheatCropDataManager::isFullyGrown)
            .collect(Collectors.toList());
    }

    /**
     * Find the nearest fully grown wheat crop within radius
     */
    public static BlockPos findNearestFullyGrownWheat(BlockPos start, int maxRadius) {
        return getFullyGrownWheatInRadius(start, maxRadius).stream()
            .min((a, b) -> Double.compare(
                start.distSqr(a),
                start.distSqr(b)))
            .orElse(null);
    }
    
    public static void clear() {
        chunkToWheatMap.clear();
        scannedChunks.clear();
    }

    /**
     * Add multiple wheat crops from a chunk at once
     */
    public static void addWheatCropsInChunk(ChunkPos chunkPos, Set<BlockPos> positions) {
        chunkToWheatMap.put(chunkPos, Collections.synchronizedSet(new HashSet<>(positions)));
        scannedChunks.add(chunkPos);
    }
} 