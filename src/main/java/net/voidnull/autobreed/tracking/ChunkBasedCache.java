package net.voidnull.autobreed.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, chunk-based caching system for tracking block positions.
 * This system efficiently manages block positions by organizing them by chunk,
 * making chunk load/unload operations efficient.
 */
public class ChunkBasedCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Map of chunk positions to sets of tracked blocks in that chunk
    private final Map<ChunkPos, Map<BlockPos, TrackedBlock>> chunkMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned
    private final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // The blocks we're tracking
    private final Set<TrackedBlock> trackedBlockTypes;
    
    public ChunkBasedCache(Set<TrackedBlock> trackedBlockTypes) {
        this.trackedBlockTypes = Collections.unmodifiableSet(new HashSet<>(trackedBlockTypes));
        LOGGER.info("Initialized ChunkBasedCache with {} block types to track", trackedBlockTypes.size());
        for (TrackedBlock type : trackedBlockTypes) {
            LOGGER.info("  - Tracking block type: {}", type.getBlock().getName());
        }
    }
    
    /**
     * Called when a chunk is loaded. Scans the chunk for tracked blocks.
     */
    public void onChunkLoad(ChunkAccess chunk, LevelAccessor level) {
        ChunkPos chunkPos = chunk.getPos();
        if (scannedChunks.contains(chunkPos)) {
            LOGGER.debug("Chunk {} already scanned, skipping", chunkPos);
            return;
        }
        
        LOGGER.debug("Scanning chunk {} for tracked blocks...", chunkPos);
        Map<BlockPos, TrackedBlock> blocksInChunk = new HashMap<>();
        
        // Scan the chunk for tracked blocks
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
                    BlockState state = chunk.getBlockState(pos);
                    
                    for (TrackedBlock trackedBlock : trackedBlockTypes) {
                        if (trackedBlock.matches(state)) {
                            LOGGER.debug("Found tracked block {} at {}", trackedBlock.getBlock().getName(), pos);
                            if (trackedBlock.onDiscovered(pos, level, state)) {
                                blocksInChunk.put(pos, trackedBlock);
                                LOGGER.info("Successfully registered {} at {}", trackedBlock.getBlock().getName(), pos);
                            }
                            break;
                        }
                    }
                }
            }
        }
        
        if (!blocksInChunk.isEmpty()) {
            chunkMap.put(chunkPos, new ConcurrentHashMap<>(blocksInChunk));
            LOGGER.info("Found {} tracked blocks in chunk {}", blocksInChunk.size(), chunkPos);
            blocksInChunk.forEach((pos, block) -> 
                LOGGER.debug("  - {} at {}", block.getBlock().getName(), pos));
        } else {
            LOGGER.debug("No tracked blocks found in chunk {}", chunkPos);
        }
        
        scannedChunks.add(chunkPos);
    }
    
    /**
     * Called when a chunk is unloaded
     */
    public void onChunkUnload(ChunkPos chunkPos) {
        Map<BlockPos, TrackedBlock> removed = chunkMap.remove(chunkPos);
        if (removed != null && !removed.isEmpty()) {
            LOGGER.debug("Unloaded {} tracked blocks from chunk {}", removed.size(), chunkPos);
        }
        scannedChunks.remove(chunkPos);
    }
    
    /**
     * Called when a block is placed
     */
    public void onBlockPlace(BlockPos pos, LevelAccessor level, BlockState state) {
        LOGGER.debug("Block placed at {}: {}", pos, state.getBlock().getName());
        for (TrackedBlock trackedBlock : trackedBlockTypes) {
            if (trackedBlock.matches(state)) {
                LOGGER.info("Placed block matches tracked type: {}", trackedBlock.getBlock().getName());
                if (trackedBlock.onDiscovered(pos, level, state)) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    chunkMap.computeIfAbsent(chunkPos, k -> new ConcurrentHashMap<>())
                           .put(pos, trackedBlock);
                    LOGGER.info("Successfully registered newly placed {} at {}", trackedBlock.getBlock().getName(), pos);
                }
                break;
            }
        }
    }
    
    /**
     * Called when a block is broken
     */
    public void onBlockBreak(BlockPos pos, LevelAccessor level, BlockState oldState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, TrackedBlock> blocksInChunk = chunkMap.get(chunkPos);
        if (blocksInChunk != null) {
            TrackedBlock trackedBlock = blocksInChunk.remove(pos);
            if (trackedBlock != null) {
                LOGGER.info("Removed tracked block {} at {}", trackedBlock.getBlock().getName(), pos);
                trackedBlock.onRemoved(pos, level);
                if (blocksInChunk.isEmpty()) {
                    chunkMap.remove(chunkPos);
                }
            }
        }
    }
    
    /**
     * Called when a block's state changes
     */
    public void onBlockChanged(BlockPos pos, LevelAccessor level, BlockState newState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<BlockPos, TrackedBlock> blocksInChunk = chunkMap.get(chunkPos);
        if (blocksInChunk != null) {
            TrackedBlock trackedBlock = blocksInChunk.get(pos);
            if (trackedBlock != null) {
                LOGGER.debug("State changed for tracked block {} at {}", trackedBlock.getBlock().getName(), pos);
                trackedBlock.onStateChanged(pos, level, newState);
            }
        }
    }
    
    /**
     * Find all tracked blocks of a specific type within radius of a position
     */
    public List<BlockPos> findBlocksInRadius(BlockPos center, int maxRadius, TrackedBlock type) {
        ChunkPos centerChunk = new ChunkPos(center);
        int chunkRadius = (maxRadius >> 4) + 1;  // Convert block radius to chunk radius
        List<BlockPos> nearbyBlocks = new ArrayList<>();
        int maxRadiusSq = maxRadius * maxRadius;

        // Check all chunks in range
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos checkChunk = new ChunkPos(
                    centerChunk.x + dx,
                    centerChunk.z + dz
                );
                
                Map<BlockPos, TrackedBlock> blocksInChunk = chunkMap.get(checkChunk);
                if (blocksInChunk != null) {
                    blocksInChunk.forEach((pos, block) -> {
                        if (block == type && center.distSqr(pos) <= maxRadiusSq) {
                            nearbyBlocks.add(pos);
                        }
                    });
                }
            }
        }

        if (!nearbyBlocks.isEmpty()) {
            LOGGER.debug("Found {} blocks of type {} within {} blocks of {}", 
                nearbyBlocks.size(), type.getBlock().getName(), maxRadius, center);
        }

        return nearbyBlocks;
    }
    
    /**
     * Find the nearest tracked block of a specific type
     */
    public BlockPos findNearest(BlockPos start, int maxRadius, TrackedBlock type) {
        BlockPos nearest = findBlocksInRadius(start, maxRadius, type).stream()
            .min((a, b) -> Double.compare(start.distSqr(a), start.distSqr(b)))
            .orElse(null);
            
        if (nearest != null) {
            LOGGER.debug("Found nearest {} at {} (distance: {})", 
                type.getBlock().getName(), nearest, Math.sqrt(start.distSqr(nearest)));
        } else {
            LOGGER.debug("No {} found within {} blocks of {}", 
                type.getBlock().getName(), maxRadius, start);
        }
        
        return nearest;
    }
    
    /**
     * Clear all cached data
     */
    public void clear() {
        int totalBlocks = chunkMap.values().stream()
            .mapToInt(Map::size)
            .sum();
        LOGGER.info("Clearing cache of {} blocks across {} chunks", 
            totalBlocks, chunkMap.size());
        chunkMap.clear();
        scannedChunks.clear();
    }
} 