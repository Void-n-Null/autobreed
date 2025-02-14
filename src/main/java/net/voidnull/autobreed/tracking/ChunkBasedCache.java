package net.voidnull.autobreed.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * A thread-safe, chunk-based caching system for tracking blocks.
 * Uses memory-efficient position storage and fast block type lookups.
 * Maintains stability across sessions while optimizing performance.
 */
public class ChunkBasedCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Map of chunk positions to maps of relative positions to tracked blocks
    private final Map<ChunkPos, Map<ChunkRelativePos, TrackedBlock>> chunkMap = new ConcurrentHashMap<>();
    
    // Keep track of which chunks we've scanned
    private final Set<ChunkPos> scannedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Stable block identification
    private final Map<ResourceLocation, TrackedBlock> stableBlockToTracker = new ConcurrentHashMap<>();
    
    // Performance optimized runtime lookups
    private final Int2ObjectMap<TrackedBlock> runtimeBlockToTracker = new Int2ObjectOpenHashMap<>();
    private final BitSet runtimeTrackedIds = new BitSet();
    private final Object2IntMap<ResourceLocation> stableToRuntimeId = new Object2IntOpenHashMap<>();
    
    public ChunkBasedCache(Set<TrackedBlock> trackedBlockTypes) {
        LOGGER.info("==========================================");
        LOGGER.info("Initializing ChunkBasedCache");
        LOGGER.info("Number of block types to track: {}", trackedBlockTypes.size());
        
        // Initialize both stable and runtime lookups
        for (TrackedBlock tracker : trackedBlockTypes) {
            Block block = tracker.getBlock();
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            
            // Store stable mapping first
            stableBlockToTracker.put(blockId, tracker);
            
            // Runtime mappings will be initialized on first use in getTracker()
            // This ensures blocks are fully initialized when we get their runtime IDs
            LOGGER.info("Registered tracker for {}:", blockId);
            LOGGER.info("  - Block Class: {}", block.getClass().getSimpleName());
            LOGGER.info("  - Tracker Class: {}", tracker.getClass().getSimpleName());
            LOGGER.info("  - Default State: {}", block.defaultBlockState());
        }
        
        LOGGER.info("Successfully registered {} block types", stableBlockToTracker.size());
        LOGGER.info("Registered blocks: {}", stableBlockToTracker.keySet());
        LOGGER.info("==========================================");
    }
    
    /**
     * Get the appropriate tracker for a block state, using fast runtime lookup
     */
    private TrackedBlock getTracker(BlockState state) {
        int runtimeId = Block.getId(state);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        
        // Log runtime ID lookup
        if (blockId.getPath().contains("hay")) {
            LOGGER.info("Getting tracker for hay block:");
            LOGGER.info("  - Runtime ID: {}", runtimeId);
            LOGGER.info("  - Block State: {}", state);
            LOGGER.info("  - Block ID: {}", blockId);
            LOGGER.info("  - Runtime tracker exists: {}", runtimeBlockToTracker.containsKey(runtimeId));
            LOGGER.info("  - Stable tracker exists: {}", stableBlockToTracker.containsKey(blockId));
            LOGGER.info("  - Runtime IDs tracked: {}", runtimeTrackedIds);
            LOGGER.info("  - Runtime trackers: {}", runtimeBlockToTracker);
            LOGGER.info("  - Stable trackers: {}", stableBlockToTracker);
        }
        
        TrackedBlock tracker = runtimeBlockToTracker.get(runtimeId);
        
        if (tracker == null) {
            // Try to get tracker from stable ID
            ResourceLocation stableId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            tracker = stableBlockToTracker.get(stableId);
            if (tracker != null) {
                // Update runtime mappings
                runtimeBlockToTracker.put(runtimeId, tracker);
                runtimeTrackedIds.set(runtimeId);
                stableToRuntimeId.put(stableId, runtimeId);
                if (blockId.getPath().contains("hay")) {
                    LOGGER.info("Initialized runtime mapping for hay block:");
                    LOGGER.info("  - Runtime ID: {}", runtimeId);
                    LOGGER.info("  - Block ID: {}", blockId);
                    LOGGER.info("  - Tracker: {}", tracker.getClass().getSimpleName());
                    LOGGER.info("  - Runtime IDs tracked: {}", runtimeTrackedIds);
                    LOGGER.info("  - Runtime trackers: {}", runtimeBlockToTracker);
                }
            }
        }
        
        return tracker;
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
        
            
            Map<ChunkRelativePos, TrackedBlock> discoveredBlocks = scanChunk(chunk, level);
            
            if (!discoveredBlocks.isEmpty()) {
                // Log summary of discovered blocks by type
                Map<ResourceLocation, Integer> blockCounts = new HashMap<>();
                discoveredBlocks.forEach((pos, tracker) -> {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(tracker.getBlock());
                    blockCounts.merge(blockId, 1, Integer::sum);
                });
                
                blockCounts.forEach((blockId, count) -> {
                    if (blockId.getPath().contains("hay")) {
                        LOGGER.info("Found {} hay bales in chunk {}", count, chunkPos);
                    } else if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Found {} {} in chunk {}", count, blockId, chunkPos);
                    }
                });
            } else if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No tracked blocks found in chunk {}", chunkPos);
            }
            
            chunkMap.put(chunkPos, new ConcurrentHashMap<>(discoveredBlocks));
            scannedChunks.add(chunkPos);
        } finally {
            PerformanceMetrics.stopTimer("chunk_scan");
        }
    }
    
    /**
     * Scans a chunk for tracked blocks and returns a map of their positions to trackers.
     */
    private Map<ChunkRelativePos, TrackedBlock> scanChunk(ChunkAccess chunk, LevelAccessor level) {
        Map<ChunkRelativePos, TrackedBlock> blocksInChunk = new HashMap<>();
        ChunkPos chunkPos = chunk.getPos();
        
        // Get chunk sections
        LevelChunkSection[] sections = chunk.getSections();
        int minSection = chunk.getMinSection();
        
        // Scan each section
        for (int sectionY = 0; sectionY < sections.length; sectionY++) {
            LevelChunkSection section = sections[sectionY];
            if (section == null || section.hasOnlyAir()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Skipping empty section {} in chunk {}", sectionY, chunkPos);
                }
                continue;
            }
            
            scanChunkSection(section, sectionY + minSection, chunkPos, level, blocksInChunk);
        }
        
        return blocksInChunk;
    }
    
    /**
     * Scans a single chunk section for tracked blocks.
     */
    private void scanChunkSection(
            LevelChunkSection section,
            int sectionY,
            ChunkPos chunkPos,
            LevelAccessor level,
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk) {
        int yOffset = sectionY << 4;  // Multiply by 16
        
        // Log section scanning at trace level
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Scanning section at Y={} in chunk {}", sectionY, chunkPos);
        }
        
        // Scan the section
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    BlockState state = section.getBlockState(x, y, z);
                    int runtimeId = Block.getId(state);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    
                    // Check if this might be a block we care about
                    if (blockId.getPath().contains("hay") || stableBlockToTracker.containsKey(blockId)) {
                        // Initialize the tracker if needed
                        TrackedBlock tracker = getTracker(state);
                        if (tracker != null) {
                            processBlockInSection(
                                state,
                                new BlockPos(
                                    chunkPos.getMinBlockX() + x,
                                    yOffset + y,
                                    chunkPos.getMinBlockZ() + z
                                ),
                                level,
                                blocksInChunk
                            );
                        }
                        continue;
                    }
                    
                    // For other blocks, use fast runtime ID check
                    if (!runtimeTrackedIds.get(runtimeId)) {
                        continue;
                    }
                    
                    processBlockInSection(
                        state,
                        new BlockPos(
                            chunkPos.getMinBlockX() + x,
                            yOffset + y,
                            chunkPos.getMinBlockZ() + z
                        ),
                        level,
                        blocksInChunk
                    );
                }
            }
        }
    }
    
    /**
     * Processes a single block during chunk scanning.
     */
    private void processBlockInSection(
            BlockState state,
            BlockPos worldPos,
            LevelAccessor level,
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk) {
        int runtimeId = Block.getId(state);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        
        TrackedBlock tracker = getTracker(state);
        if (tracker == null) {
            if (blockId.getPath().contains("hay")) {
                LOGGER.warn("Failed to get tracker for hay block at {}", worldPos);
                LOGGER.warn("  - Runtime ID: {}", runtimeId);
                LOGGER.warn("  - Block State: {}", state);
                LOGGER.warn("  - Block ID: {}", blockId);
            }
            return;
        }
        
        if (!tracker.matches(state)) {
            if (blockId.getPath().contains("hay")) {
                LOGGER.warn("Hay block failed matches() check at {}", worldPos);
            }
            return;
        }
        
        if (!tracker.onDiscovered(worldPos, level, state)) {
            if (blockId.getPath().contains("hay")) {
                LOGGER.warn("Hay block failed discovery check at {}", worldPos);
            }
            return;
        }
        
        ChunkRelativePos relPos = ChunkRelativePos.fromBlockPos(worldPos);
        blocksInChunk.put(relPos, tracker);
        if (blockId.getPath().contains("hay")) {
            LOGGER.info("Successfully discovered hay bale at {}", worldPos);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Discovered {} at {}", blockId, worldPos);
        }
    }
    
    /**
     * Called when a chunk is unloaded
     */
    public void onChunkUnload(ChunkPos chunkPos) {
        PerformanceMetrics.startTimer("chunk_unload");
        try {
            // Get the blocks in the chunk before removing
            Map<ChunkRelativePos, TrackedBlock> blocksInChunk = chunkMap.remove(chunkPos);
            
            if (blocksInChunk != null && !blocksInChunk.isEmpty()) {
                LOGGER.debug("Unloading chunk {} with {} tracked blocks", chunkPos, blocksInChunk.size());
                
                // Notify trackers about all blocks being unloaded
                blocksInChunk.forEach((relPos, tracker) -> {
                    try {
                        BlockPos worldPos = relPos.toBlockPos(chunkPos);
                        tracker.onRemoved(worldPos, null);  // level is null since chunk is unloaded
                    } catch (Exception e) {
                        LOGGER.error("Error notifying tracker about block removal during chunk unload at {}: {}", 
                            relPos.toBlockPos(chunkPos), e.getMessage());
                    }
                });
                
                // Clear the map to help GC
                blocksInChunk.clear();
            }
            
            scannedChunks.remove(chunkPos);
            
            // Periodically clean up runtime mappings (every 100 chunk unloads)
            if (Math.random() < 0.01) {  // 1% chance to trigger cleanup
                cleanupRuntimeMappings();
            }
        } finally {
            PerformanceMetrics.stopTimer("chunk_unload");
        }
    }
    
    /**
     * Cleanup runtime block mappings that are no longer in use
     */
    private void cleanupRuntimeMappings() {
        PerformanceMetrics.startTimer("runtime_cleanup");
        try {
            LOGGER.debug("Starting runtime mappings cleanup");
            int beforeSize = runtimeBlockToTracker.size();
            
            // Get all currently tracked blocks
            Set<TrackedBlock> activeTrackers = new HashSet<>();
            chunkMap.values().forEach(chunk -> activeTrackers.addAll(chunk.values()));
            
            // Remove runtime mappings for blocks we're not tracking
            runtimeBlockToTracker.values().removeIf(tracker -> !activeTrackers.contains(tracker));
            
            // Clean up the runtime IDs bitset
            runtimeTrackedIds.clear();
            runtimeBlockToTracker.keySet().forEach(runtimeTrackedIds::set);
            
            // Clean up stable to runtime mappings
            stableToRuntimeId.values().removeIf(runtimeId -> !runtimeBlockToTracker.containsKey(runtimeId));
            
            int removedCount = beforeSize - runtimeBlockToTracker.size();
            if (removedCount > 0) {
                LOGGER.info("Cleaned up {} stale runtime mappings", removedCount);
            }
        } catch (Exception e) {
            LOGGER.error("Error during runtime mappings cleanup: {}", e.getMessage());
        } finally {
            PerformanceMetrics.stopTimer("runtime_cleanup");
        }
    }
    
    /**
     * Called when a block is placed
     */
    public void onBlockPlace(BlockPos pos, LevelAccessor level, BlockState state) {
        PerformanceMetrics.startTimer("block_place");
        try {
            TrackedBlock tracker = getTracker(state);
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
        PerformanceMetrics.startTimer("cache_clear");
        try {
            LOGGER.info("Clearing all cached data");
            
            // Notify trackers about all blocks being removed
            chunkMap.forEach((chunkPos, blocksInChunk) -> {
                blocksInChunk.forEach((relPos, tracker) -> {
                    try {
                        BlockPos worldPos = relPos.toBlockPos(chunkPos);
                        tracker.onRemoved(worldPos, null);
                    } catch (Exception e) {
                        LOGGER.error("Error notifying tracker during cache clear at {}: {}", 
                            relPos.toBlockPos(chunkPos), e.getMessage());
                    }
                });
                blocksInChunk.clear();
            });
            
            // Clear all maps
            chunkMap.clear();
            scannedChunks.clear();
            
            // Clear runtime mappings
            runtimeBlockToTracker.clear();
            runtimeTrackedIds.clear();
            stableToRuntimeId.clear();
            
            LOGGER.info("Cache cleared successfully");
        } finally {
            PerformanceMetrics.stopTimer("cache_clear");
        }
    }
} 