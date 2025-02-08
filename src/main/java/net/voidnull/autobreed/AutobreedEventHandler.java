package net.voidnull.autobreed;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.NbtAccounter;

public class AutobreedEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Path savePath;

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().is(Blocks.HAY_BLOCK)) {
            if (event.getLevel() instanceof ServerLevel) {
                HayBaleDataManager.addHayBale(event.getPos());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getState().is(Blocks.HAY_BLOCK)) {
            if (event.getLevel() instanceof ServerLevel) {
                HayBaleDataManager.removeHayBale(event.getPos());
                // You might need to send a packet here to sync removal on clients,
                // depending on how you handle the "eating" logic.
            }
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            // Remove entries for all hay bales in the unloaded chunk.
            LevelChunk chunk = (LevelChunk) event.getChunk();
            // Iterate through all BlockPos in the chunk and check if they are in the map
            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                    for (int y = event.getLevel().getMinBuildHeight(); y < event.getLevel().getMaxBuildHeight(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (HayBaleDataManager.getEatenCounts().containsKey(pos)) {
                            HayBaleDataManager.removeHayBale(pos);
                        }
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            savePath = serverLevel.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(AutoBreed.MOD_ID + "_haybale_data.dat");

            try {
                if (Files.exists(savePath)) {
                    CompoundTag loadedData = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());
                    ListTag hayBaleList = loadedData.getList("HayBales", Tag.TAG_COMPOUND);
                    for (int i = 0; i < hayBaleList.size(); i++) {
                        CompoundTag hayBaleTag = hayBaleList.getCompound(i);
                        BlockPos pos = new BlockPos(hayBaleTag.getInt("X"), hayBaleTag.getInt("Y"), hayBaleTag.getInt("Z"));
                        int eatenCount = hayBaleTag.getInt("EatenCount");
                        HayBaleDataManager.getEatenCounts().put(pos, eatenCount);
                    }
                    LOGGER.info("Hay bale data loaded.");
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load hay bale data", e);
            }
        }
    }

     @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel) {
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
                NbtIo.writeCompressed(saveData, savePath);
                 LOGGER.info("Hay bale data saved.");
            } catch (IOException e) {
                LOGGER.error("Failed to save hay bale data", e);
            }
        }
    }
}