package net.voidnull.autobreed.tracking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Unified event handler for all block tracking.
 * This handler manages the chunk-based caching system and coordinates
 * all block tracking activities.
 */
public class BlockTrackingHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockTrackingHandler.class);
    
    // The main cache that handles all block tracking
    private final ChunkBasedCache blockCache;
    
    // Keep references to our tracked blocks for easy access
    private final TrackedHayBale hayBaleTracker;
    private final Map<CropType, TrackedCrop> cropTrackers;
    
    public BlockTrackingHandler() {
        LOGGER.info("Initializing BlockTrackingHandler");
        
        // Create our trackers
        hayBaleTracker = new TrackedHayBale();
        LOGGER.info("Created hay bale tracker:");
        LOGGER.info("  - Block: {}", hayBaleTracker.getBlock());
        LOGGER.info("  - Block ID: {}", BuiltInRegistries.BLOCK.getKey(hayBaleTracker.getBlock()));
        
        cropTrackers = new EnumMap<>(CropType.class);
        for (CropType cropType : CropType.values()) {
            TrackedCrop tracker = new TrackedCrop(cropType);
            cropTrackers.put(cropType, tracker);
            LOGGER.info("Created crop tracker for {}:", cropType);
            LOGGER.info("  - Block: {}", tracker.getBlock());
            LOGGER.info("  - Block ID: {}", BuiltInRegistries.BLOCK.getKey(tracker.getBlock()));
        }
        
        // Create the set of all trackers
        Set<TrackedBlock> allTrackers = new HashSet<>();
        allTrackers.add(hayBaleTracker);
        allTrackers.addAll(cropTrackers.values());
        
        LOGGER.info("Initializing cache with {} trackers:", allTrackers.size());
        allTrackers.forEach(tracker -> 
            LOGGER.info("  - {}: {}", 
                tracker.getClass().getSimpleName(),
                BuiltInRegistries.BLOCK.getKey(tracker.getBlock())));
        
        // Initialize the cache with all our trackers
        blockCache = new ChunkBasedCache(allTrackers);
    }
    
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        ChunkPos pos = event.getChunk().getPos();
        
        try {
            blockCache.onChunkLoad(event.getChunk(), event.getLevel());
        } catch (Exception e) {
            LOGGER.error("Error processing chunk load at {}: {}", pos, e.getMessage(), e);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.onBlockPlace(event.getPos(), event.getLevel(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.onBlockBreak(event.getPos(), event.getLevel(), event.getState());
    }

    @SubscribeEvent
    public void onBlockChange(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        BlockState newState = event.getLevel().getBlockState(event.getPos());
        // Only track changes if the block state actually changed
        BlockState oldState = event.getState();
        if (!newState.equals(oldState)) {
            LOGGER.debug("Block changed at {} from {} to {}", 
                event.getPos(), 
                BuiltInRegistries.BLOCK.getKey(oldState.getBlock()),
                BuiltInRegistries.BLOCK.getKey(newState.getBlock()));
            blockCache.onBlockChanged(event.getPos(), event.getLevel(), newState);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.clear();
        // Log final stats before world unloads
        PerformanceMetrics.logStats();
    }
    
    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        // Log performance stats on world save
        PerformanceMetrics.logStats();
    }
    
    // Helper methods to access trackers
    public TrackedHayBale getHayBaleTracker() {
        return hayBaleTracker;
    }
    
    public TrackedCrop getCropTracker(CropType type) {
        return cropTrackers.get(type);
    }
    
    public ChunkBasedCache getBlockCache() {
        return blockCache;
    }
} 