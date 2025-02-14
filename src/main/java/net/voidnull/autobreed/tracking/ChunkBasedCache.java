package net.voidnull.autobreed.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;

/**
 * A thread-safe, chunk-based caching system for tracking blocks.
 * Uses memory-efficient position storage and fast block type lookups.
 */
public class ChunkBasedCache {
    // Map of chunk positions to maps of relative positions to tracked blocks
    private final Map<ChunkPos, Map<ChunkRelativePos, TrackedBlock>> chunkMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned
    private final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Fast lookup of block ID to tracker
    private final Int2ObjectMap<TrackedBlock> blockIdToTracker = new Int2ObjectOpenHashMap<>();
    
    // BitSet for ultra-fast block type checking
    private final BitSet trackedBlockIds = new BitSet();
    
    public ChunkBasedCache(Set<TrackedBlock> trackedBlockTypes) {
        // Initialize fast block lookup and bitmap
        for (TrackedBlock tracker : trackedBlockTypes) {
            int blockId = Block.getId(tracker.getBlock().defaultBlockState());
            blockIdToTracker.put(blockId, tracker);
            trackedBlockIds.set(blockId);
        }
    }
    
    /**
     * Called when a chunk is loaded. Scans the chunk for tracked blocks.
     */
    public void onChunkLoad(ChunkAccess chunk, LevelAccessor level) {
        PerformanceMetrics.startTimer("chunk_scan");
        try {
            ChunkPos chunkPos = chunk.getPos();
            if (scannedChunks.contains(chunkPos)) {
                return;
            }
            
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk = new HashMap<>();
            
            // Get chunk sections
            LevelChunkSection[] sections = chunk.getSections();
            int minSection = chunk.getMinSection();
            
            // Scan each section
            for (int sectionY = 0; sectionY < sections.length; sectionY++) {
                LevelChunkSection section = sections[sectionY];
                if (section == null || section.hasOnlyAir()) continue;
                
                int yOffset = (minSection + sectionY) << 4;
                
                // Scan the section
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 16; y++) {
                            BlockState state = section.getBlockState(x, y, z);
                            int blockId = Block.getId(state);
                            
                            // Ultra-fast check if we care about this block type
                            if (!trackedBlockIds.get(blockId)) continue;
                            
                            TrackedBlock tracker = blockIdToTracker.get(blockId);
                            if (tracker.matches(state)) {
                                BlockPos worldPos = new BlockPos(
                                    chunkPos.getMinBlockX() + x,
                                    yOffset + y,
                                    chunkPos.getMinBlockZ() + z
                                );
                                
                                if (tracker.onDiscovered(worldPos, level, state)) {
                                    ChunkRelativePos relPos = ChunkRelativePos.fromBlockPos(worldPos);
                                    blocksInChunk.put(relPos, tracker);
                                }
                            }
                        }
                    }
                }
            }
            
            if (!blocksInChunk.isEmpty()) {
                chunkMap.put(chunkPos, new ConcurrentHashMap<>(blocksInChunk));
            }
            
            scannedChunks.add(chunkPos);
        } finally {
            PerformanceMetrics.stopTimer("chunk_scan");
        }
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
        PerformanceMetrics.startTimer("block_place");
        try {
            TrackedBlock tracker = blockIdToTracker.get(Block.getId(state));
            if (tracker != null && tracker.matches(state) && tracker.onDiscovered(pos, level, state)) {
                ChunkPos chunkPos = new ChunkPos(pos);
                ChunkRelativePos relPos = ChunkRelativePos.fromBlockPos(pos);
                chunkMap.computeIfAbsent(chunkPos, k -> new ConcurrentHashMap<>())
                       .put(relPos, tracker);
            }
        } finally {
            PerformanceMetrics.stopTimer("block_place");
        }
    }
    
    /**
     * Called when a block is broken
     */
    public void onBlockBreak(BlockPos pos, LevelAccessor level, BlockState oldState) {
        PerformanceMetrics.startTimer("block_break");
        try {
            ChunkPos chunkPos = new ChunkPos(pos);
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk = chunkMap.get(chunkPos);
            if (blocksInChunk != null) {
                ChunkRelativePos relPos = ChunkRelativePos.fromBlockPos(pos);
                TrackedBlock tracker = blocksInChunk.remove(relPos);
                if (tracker != null) {
                    tracker.onRemoved(pos, level);
                    if (blocksInChunk.isEmpty()) {
                        chunkMap.remove(chunkPos);
                    }
                }
            }
        } finally {
            PerformanceMetrics.stopTimer("block_break");
        }
    }
    
    /**
     * Called when a block's state changes
     */
    public void onBlockChanged(BlockPos pos, LevelAccessor level, BlockState newState) {
        PerformanceMetrics.startTimer("block_change");
        try {
            ChunkPos chunkPos = new ChunkPos(pos);
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk = chunkMap.get(chunkPos);
            if (blocksInChunk != null) {
                ChunkRelativePos relPos = ChunkRelativePos.fromBlockPos(pos);
                TrackedBlock tracker = blocksInChunk.get(relPos);
                if (tracker != null) {
                    tracker.onStateChanged(pos, level, newState);
                }
            }
        } finally {
            PerformanceMetrics.stopTimer("block_change");
        }
    }
    
    /**
     * Find all tracked blocks of a specific type within radius of a position
     */
    public List<BlockPos> findBlocksInRadius(BlockPos center, int maxRadius, TrackedBlock type) {
        PerformanceMetrics.startTimer("radius_search");
        try {
            ChunkPos centerChunk = new ChunkPos(center);
            int chunkRadius = (maxRadius >> 4) + 1;  // Convert block radius to chunk radius
            List<BlockPos> nearbyBlocks = new ArrayList<>();
            int maxRadiusSq = maxRadius * maxRadius;

            // Check all chunks in range
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    // Skip chunks that are definitely out of range
                    if (dx * dx + dz * dz > (chunkRadius + 1) * (chunkRadius + 1)) continue;
                    
                    ChunkPos checkChunk = new ChunkPos(
                        centerChunk.x + dx,
                        centerChunk.z + dz
                    );
                    
                    Map<ChunkRelativePos, TrackedBlock> blocksInChunk = chunkMap.get(checkChunk);
                    if (blocksInChunk != null) {
                        blocksInChunk.forEach((relPos, block) -> {
                            if (block == type) {
                                BlockPos worldPos = relPos.toBlockPos(checkChunk);
                                if (center.distSqr(worldPos) <= maxRadiusSq) {
                                    nearbyBlocks.add(worldPos);
                                }
                            }
                        });
                    }
                }
            }

            return nearbyBlocks;
        } finally {
            PerformanceMetrics.stopTimer("radius_search");
        }
    }
    
    /**
     * Find the nearest tracked block of a specific type
     */
    public BlockPos findNearest(BlockPos start, int maxRadius, TrackedBlock type) {
        PerformanceMetrics.startTimer("find_nearest");
        try {
            return findBlocksInRadius(start, maxRadius, type).stream()
                .min((a, b) -> Double.compare(start.distSqr(a), start.distSqr(b)))
                .orElse(null);
        } finally {
            PerformanceMetrics.stopTimer("find_nearest");
        }
    }
    
    /**
     * Clear all cached data
     */
    public void clear() {
        chunkMap.clear();
        scannedChunks.clear();
    }
} 