package net.voidnull.autobreed.tracking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.*;

/**
 * Unified event handler for all block tracking.
 * This handler manages the chunk-based caching system and coordinates
 * all block tracking activities.
 */
public class BlockTrackingHandler {
    // The main cache that handles all block tracking
    private final ChunkBasedCache blockCache;
    
    // Keep references to our tracked blocks for easy access
    private final TrackedHayBale hayBaleTracker;
    private final Map<CropType, TrackedCrop> cropTrackers;
    
    public BlockTrackingHandler() {
        // Create our trackers
        hayBaleTracker = new TrackedHayBale();
        cropTrackers = new EnumMap<>(CropType.class);
        for (CropType cropType : CropType.values()) {
            cropTrackers.put(cropType, new TrackedCrop(cropType));
        }
        
        // Create the set of all trackers
        Set<TrackedBlock> allTrackers = new HashSet<>();
        allTrackers.add(hayBaleTracker);
        allTrackers.addAll(cropTrackers.values());
        
        // Initialize the cache with all our trackers
        blockCache = new ChunkBasedCache(allTrackers);
    }
    
    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.onChunkLoad(event.getChunk(), event.getLevel());
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