package net.voidnull.autobreed;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
 * Event handler for wheat crop management.
 * This handler manages the caching system and data persistence for wheat crops,
 * tracking their positions and growth states efficiently.
 */
public class WheatCropEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Path savePath;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        WheatCropCache.onChunkLoad(event.getChunk());
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        WheatCropCache.onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        if (event.getPlacedBlock().is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Wheat crop placed at {}", pos);
            WheatCropCache.addWheatCrop(pos);
            WheatCropDataManager.addWheatCrop(pos);
            // Update growth state immediately
            if (event.getLevel() instanceof Level level) {
                WheatCropDataManager.updateGrowthState(pos, level);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        if (event.getState().is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Wheat crop broken at {}", pos);
            WheatCropCache.removeWheatCrop(pos);
            WheatCropDataManager.removeWheatCrop(pos);
        }
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        if (event.getState().is(Blocks.WHEAT) && event.getLevel() instanceof Level level) {
            BlockPos pos = event.getPos();
            WheatCropDataManager.updateGrowthState(pos, level);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        savePath = serverLevel.getServer().getWorldPath(LevelResource.ROOT)
                            .resolve("data")
                            .resolve(AutoBreed.MODID + "_wheat_data.dat");

        try {
            if (Files.exists(savePath)) {
                CompoundTag loadedData = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());
                ListTag wheatList = loadedData.getList("WheatCrops", Tag.TAG_COMPOUND);
                
                for (int i = 0; i < wheatList.size(); i++) {
                    CompoundTag wheatTag = wheatList.getCompound(i);
                    BlockPos pos = new BlockPos(
                        wheatTag.getInt("X"),
                        wheatTag.getInt("Y"),
                        wheatTag.getInt("Z")
                    );
                    boolean isGrown = wheatTag.getBoolean("IsGrown");
                    WheatCropDataManager.getWheatCrops().put(pos, isGrown);
                    
                    // Also add to cache if the block still exists
                    if (serverLevel.getBlockState(pos).is(Blocks.WHEAT)) {
                        WheatCropCache.addWheatCrop(pos);
                        WheatCropDataManager.updateGrowthState(pos, serverLevel);
                    }
                }
                LOGGER.info("Wheat crop data loaded successfully");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load wheat crop data", e);
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        
        CompoundTag saveData = new CompoundTag();
        ListTag wheatList = new ListTag();

        for (Map.Entry<BlockPos, Boolean> entry : WheatCropDataManager.getWheatCrops().entrySet()) {
            CompoundTag wheatTag = new CompoundTag();
            wheatTag.putInt("X", entry.getKey().getX());
            wheatTag.putInt("Y", entry.getKey().getY());
            wheatTag.putInt("Z", entry.getKey().getZ());
            wheatTag.putBoolean("IsGrown", entry.getValue());
            wheatList.add(wheatTag);
        }
        
        saveData.put("WheatCrops", wheatList);

        try {
            Files.createDirectories(savePath.getParent());
            NbtIo.writeCompressed(saveData, savePath);
            LOGGER.info("Wheat crop data saved successfully");
        } catch (IOException e) {
            LOGGER.error("Failed to save wheat crop data", e);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        LOGGER.debug("World unloading, clearing wheat crop cache");
        WheatCropCache.clear();
    }
} 