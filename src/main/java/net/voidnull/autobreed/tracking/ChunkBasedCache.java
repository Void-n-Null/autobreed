package net.voidnull.autobreed.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe, chunk-based caching system for tracking block positions.
 * This system efficiently manages block positions by organizing them by chunk,
 * making chunk load/unload operations efficient.
 */
public class ChunkBasedCache {
    // Map of chunk positions to sets of tracked blocks in that chunk
    private final Map<ChunkPos, Map<BlockPos, TrackedBlock>> chunkMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned
    private final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // The blocks we're tracking
    private final Set<TrackedBlock> trackedBlockTypes;
    
    public ChunkBasedCache(Set<TrackedBlock> trackedBlockTypes) {
        this.trackedBlockTypes = Collections.unmodifiableSet(new HashSet<>(trackedBlockTypes));
    }
    
    /**
     * Called when a chunk is loaded. Scans the chunk for tracked blocks.
     */
    public void onChunkLoad(ChunkAccess chunk, LevelAccessor level) {
        ChunkPos chunkPos = chunk.getPos();
        if (scannedChunks.contains(chunkPos)) {
            return;
        }
        
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
                            if (trackedBlock.onDiscovered(pos, level, state)) {
                                blocksInChunk.put(pos, trackedBlock);
                            }
                            break;
                        }
                    }
                }
            }
        }
        
        if (!blocksInChunk.isEmpty()) {
            chunkMap.put(chunkPos, new ConcurrentHashMap<>(blocksInChunk));
        }
        
        scannedChunks.add(chunkPos);
    }
    
    /**
     * Called when a chunk is unloaded
     */
    public void onChunkUnload(ChunkPos chunkPos) {
        chunkMap.remove(chunkPos);
        scannedChunks.remove(chunkPos);
    }
    
    /**
     * Called when a block is placed
     */
    public void onBlockPlace(BlockPos pos, LevelAccessor level, BlockState state) {
        for (TrackedBlock trackedBlock : trackedBlockTypes) {
            if (trackedBlock.matches(state)) {
                if (trackedBlock.onDiscovered(pos, level, state)) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    chunkMap.computeIfAbsent(chunkPos, k -> new ConcurrentHashMap<>())
                           .put(pos, trackedBlock);
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

        return nearbyBlocks;
    }
    
    /**
     * Find the nearest tracked block of a specific type
     */
    public BlockPos findNearest(BlockPos start, int maxRadius, TrackedBlock type) {
        return findBlocksInRadius(start, maxRadius, type).stream()
            .min((a, b) -> Double.compare(start.distSqr(a), start.distSqr(b)))
            .orElse(null);
    }
    
    /**
     * Clear all cached data
     */
    public void clear() {
        chunkMap.clear();
        scannedChunks.clear();
    }
} 