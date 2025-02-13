package net.voidnull.autobreed.tracking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.voidnull.autobreed.AutoBreed;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Unified event handler for all block tracking.
 * This handler manages the chunk-based caching system and coordinates
 * all block tracking activities.
 */
public class BlockTrackingHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Path savePath;
    
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
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        savePath = serverLevel.getServer().getWorldPath(LevelResource.ROOT)
                            .resolve("data")
                            .resolve(AutoBreed.MODID + "_block_data.dat");

        try {
            if (Files.exists(savePath)) {
                CompoundTag loadedData = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());
                
                // Load hay bale data
                if (loadedData.contains("HayBales")) {
                    ListTag hayBaleList = loadedData.getList("HayBales", Tag.TAG_COMPOUND);
                    for (int i = 0; i < hayBaleList.size(); i++) {
                        CompoundTag hayBaleTag = hayBaleList.getCompound(i);
                        BlockPos pos = new BlockPos(
                            hayBaleTag.getInt("X"),
                            hayBaleTag.getInt("Y"),
                            hayBaleTag.getInt("Z")
                        );
                        if (serverLevel.getBlockState(pos).is(Blocks.HAY_BLOCK)) {
                            int eatenCount = hayBaleTag.getInt("EatenCount");
                            for (int j = 0; j < eatenCount; j++) {
                                hayBaleTracker.consumeHayBale(pos);
                            }
                        }
                    }
                }
                
                // Load crop data
                if (loadedData.contains("Crops")) {
                    CompoundTag cropsTag = loadedData.getCompound("Crops");
                    for (CropType cropType : CropType.values()) {
                        String cropName = cropType.name();
                        if (cropsTag.contains(cropName)) {
                            ListTag cropList = cropsTag.getList(cropName, Tag.TAG_COMPOUND);
                            TrackedCrop tracker = cropTrackers.get(cropType);
                            for (int i = 0; i < cropList.size(); i++) {
                                CompoundTag cropTag = cropList.getCompound(i);
                                BlockPos pos = new BlockPos(
                                    cropTag.getInt("X"),
                                    cropTag.getInt("Y"),
                                    cropTag.getInt("Z")
                                );
                                if (serverLevel.getBlockState(pos).is(cropType.getCropBlock())) {
                                    tracker.onDiscovered(pos, serverLevel, serverLevel.getBlockState(pos));
                                }
                            }
                        }
                    }
                }
                
                LOGGER.info("Block tracking data loaded successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load block tracking data", e);
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        
        CompoundTag saveData = new CompoundTag();
        
        // Save hay bale data
        ListTag hayBaleList = new ListTag();
        blockCache.findBlocksInRadius(BlockPos.ZERO, Integer.MAX_VALUE, hayBaleTracker)
            .forEach(pos -> {
                CompoundTag hayBaleTag = new CompoundTag();
                hayBaleTag.putInt("X", pos.getX());
                hayBaleTag.putInt("Y", pos.getY());
                hayBaleTag.putInt("Z", pos.getZ());
                hayBaleTag.putInt("EatenCount", hayBaleTracker.getEatenCount(pos));
                hayBaleList.add(hayBaleTag);
            });
        saveData.put("HayBales", hayBaleList);
        
        // Save crop data
        CompoundTag cropsTag = new CompoundTag();
        for (Map.Entry<CropType, TrackedCrop> entry : cropTrackers.entrySet()) {
            ListTag cropList = new ListTag();
            blockCache.findBlocksInRadius(BlockPos.ZERO, Integer.MAX_VALUE, entry.getValue())
                .forEach(pos -> {
                    CompoundTag cropTag = new CompoundTag();
                    cropTag.putInt("X", pos.getX());
                    cropTag.putInt("Y", pos.getY());
                    cropTag.putInt("Z", pos.getZ());
                    cropList.add(cropTag);
                });
            cropsTag.put(entry.getKey().name(), cropList);
        }
        saveData.put("Crops", cropsTag);

        try {
            Files.createDirectories(savePath.getParent());
            NbtIo.writeCompressed(saveData, savePath);
            LOGGER.info("Block tracking data saved successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to save block tracking data", e);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        blockCache.clear();
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