package net.voidnull.autobreed;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * Event handler for wheat crop management.
 * This handler manages the caching system for wheat crops,
 * tracking their positions and growth states efficiently in memory.
 */
public class WheatCropEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        // First scan the chunk for wheat crops
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        Set<BlockPos> wheatPositions = new HashSet<>();
        
        // Scan the chunk for wheat crops
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
                    if (state.is(Blocks.WHEAT)) {
                        wheatPositions.add(pos);
                        // Add to data manager if not already tracked
                        if (!WheatCropDataManager.getWheatCrops().containsKey(pos)) {
                            WheatCropDataManager.addWheatCrop(pos);
                        }
                        // Update growth state
                        if (event.getLevel() instanceof Level level) {
                            WheatCropDataManager.updateGrowthState(pos, level, state);
                        }
                    }
                }
            }
        }
        
        // Add found wheat to cache
        if (!wheatPositions.isEmpty()) {
            LOGGER.debug("Found {} wheat crops in chunk {}", wheatPositions.size(), chunkPos);
            WheatCropCache.addWheatCropsInChunk(chunkPos, wheatPositions);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        WheatCropCache.onChunkUnload(event.getChunk().getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        BlockState state = event.getPlacedBlock();
        if (state.is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Wheat crop placed at {}", pos);
            WheatCropCache.addWheatCrop(pos);
            WheatCropDataManager.addWheatCrop(pos);
            // Update growth state immediately with the placed state
            if (event.getLevel() instanceof Level level) {
                WheatCropDataManager.updateGrowthState(pos, level, state);
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
        
        BlockState state = event.getState();
        if (state.is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            // Pass the current state directly rather than getting it from the world again
            if (event.getLevel() instanceof Level level) {
                WheatCropDataManager.updateGrowthState(pos, level, state);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        
        // Clear existing data - the cache will rebuild as chunks load
        WheatCropCache.clear();
        WheatCropDataManager.clear();
        LOGGER.info("World load - Cache cleared and will rebuild as chunks load");
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        LOGGER.debug("World unloading, clearing wheat crop cache");
        WheatCropCache.clear();
        WheatCropDataManager.clear();
    }

    @SubscribeEvent
    public void onCropGrowPre(CropGrowEvent.Pre event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        BlockState state = event.getState();
        if (state.is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Wheat crop attempting to grow at {}", pos);
        }
    }

    @SubscribeEvent
    public void onCropGrowPost(CropGrowEvent.Post event) {
        if (event.getLevel().isClientSide()) return;  // Server-side only
        
        BlockState newState = event.getState();
        BlockState originalState = event.getOriginalState();
        if (newState.is(Blocks.WHEAT)) {
            BlockPos pos = event.getPos();
            LOGGER.debug("Wheat crop grew at {} from age {} to {}", 
                pos,
                originalState.getValue(CropBlock.AGE),
                newState.getValue(CropBlock.AGE));
            
            // Update the growth state with the new state
            if (event.getLevel() instanceof Level level) {
                WheatCropDataManager.updateGrowthState(pos, level, newState);
            }
        }
    }
} 