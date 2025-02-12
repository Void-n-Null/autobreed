package net.voidnull.autobreed;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WheatCropDataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WheatCropDataManager.class);

    // Thread-safe map to track wheat crop positions
    private static final Map<BlockPos, Boolean> wheatCrops = Collections.synchronizedMap(new HashMap<>());

    public static void addWheatCrop(BlockPos pos) {
        wheatCrops.put(pos, false); // Initialize as not fully grown
    }

    public static void removeWheatCrop(BlockPos pos) {
        wheatCrops.remove(pos);
    }

    public static boolean isFullyGrown(BlockPos pos) {
        return wheatCrops.getOrDefault(pos, false);
    }

    public static void updateGrowthState(BlockPos pos, Level level) {
        if (!wheatCrops.containsKey(pos)) {
            LOGGER.debug("Tried to update growth state for untracked wheat crop at {}", pos);
            return;
        }
        
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WHEAT)) {
            CropBlock cropBlock = (CropBlock) Blocks.WHEAT;
            boolean isFullyGrown = cropBlock.isMaxAge(state);
            boolean wasFullyGrown = wheatCrops.get(pos);
            wheatCrops.put(pos, isFullyGrown);
            
            if (isFullyGrown != wasFullyGrown) {
                LOGGER.debug("Wheat crop at {} growth state changed: isFullyGrown={}", pos, isFullyGrown);
            }
        }
    }

    public static void consumeWheatCrop(BlockPos pos, Level level) {
        if (!wheatCrops.containsKey(pos)) return;
        
        if (level instanceof ServerLevel) {
            // Reset the crop to age 0 (just planted)
            BlockState currentState = level.getBlockState(pos);
            if (currentState.is(Blocks.WHEAT)) {
                BlockState newState = currentState.setValue(CropBlock.AGE, 0);
                level.setBlock(pos, newState, 3);
                wheatCrops.put(pos, false);
            }
        }
    }

    public static Map<BlockPos, Boolean> getWheatCrops() {
        return wheatCrops;
    }

    public static void clear() {
        wheatCrops.clear();
    }
} 