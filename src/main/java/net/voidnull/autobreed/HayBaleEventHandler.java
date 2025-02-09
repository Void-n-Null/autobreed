package net.voidnull.autobreed;

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
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Unified event handler for hay bale management.
 * This handler combines both the caching system and data persistence,
 * ensuring that hay bales are properly tracked, their eaten states are
 * saved, and chunk loading/unloading is handled efficiently.
 * 
 * The handler uses a chunk-based caching system to avoid expensive
 * block scanning operations during runtime, while maintaining data
 * consistency with the save system.
 */
public class HayBaleEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Path savePath;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        HayBaleCache.onChunkLoad(event.getChunk());
    }

    /**
     * Handles chunk unloading by efficiently removing hay bale data.
     * Unlike the previous implementation which scanned every block in the chunk,
     * this uses the cache to instantly know which hay bales were in the chunk,
     * significantly reducing server load during chunk unloads.
     */
    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        HayBaleCache.onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        if (event.getPlacedBlock().is(Blocks.HAY_BLOCK)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Hay block placed at {}", pos);
            HayBaleCache.addHayBale(pos);
            HayBaleDataManager.addHayBale(pos);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        if (event.getState().is(Blocks.HAY_BLOCK)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Hay block broken at {}", pos);
            HayBaleCache.removeHayBale(pos);
            HayBaleDataManager.removeHayBale(pos);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        savePath = serverLevel.getServer().getWorldPath(LevelResource.ROOT)
                            .resolve("data")
                            .resolve(AutoBreed.MODID + "_haybale_data.dat");

        try {
            if (Files.exists(savePath)) {
                CompoundTag loadedData = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());
                ListTag hayBaleList = loadedData.getList("HayBales", Tag.TAG_COMPOUND);
                
                for (int i = 0; i < hayBaleList.size(); i++) {
                    CompoundTag hayBaleTag = hayBaleList.getCompound(i);
                    BlockPos pos = new BlockPos(
                        hayBaleTag.getInt("X"),
                        hayBaleTag.getInt("Y"),
                        hayBaleTag.getInt("Z")
                    );
                    int eatenCount = hayBaleTag.getInt("EatenCount");
                    HayBaleDataManager.getEatenCounts().put(pos, eatenCount);
                    
                    // Also add to cache if the block still exists
                    if (serverLevel.getBlockState(pos).is(Blocks.HAY_BLOCK)) {
                        HayBaleCache.addHayBale(pos);
                    }
                }
                LOGGER.info("Hay bale data loaded successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load hay bale data", e);
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        
        CompoundTag saveData = new CompoundTag();
        ListTag hayBaleList = new ListTag();

        for (Map.Entry<BlockPos, Integer> entry : HayBaleDataManager.getEatenCounts().entrySet()) {
            CompoundTag hayBaleTag = new CompoundTag();
            hayBaleTag.putInt("X", entry.getKey().getX());
            hayBaleTag.putInt("Y", entry.getKey().getY());
            hayBaleTag.putInt("Z", entry.getKey().getZ());
            hayBaleTag.putInt("EatenCount", entry.getValue());
            hayBaleList.add(hayBaleTag);
        }
        
        saveData.put("HayBales", hayBaleList);

        try {
            Files.createDirectories(savePath.getParent());
            NbtIo.writeCompressed(saveData, savePath);
            LOGGER.info("Hay bale data saved successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to save hay bale data", e);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        LOGGER.debug("World unloading, clearing hay bale cache");
        HayBaleCache.clear();
    }
} 