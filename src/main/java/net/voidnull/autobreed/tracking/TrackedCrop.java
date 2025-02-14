package net.voidnull.autobreed.tracking;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.voidnull.autobreed.AutoBreed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackedCrop implements TrackedBlock {
    private final CropType cropType;
    private final Map<BlockPos, Boolean> growthStates = new ConcurrentHashMap<>();
    
    public TrackedCrop(CropType cropType) {
        this.cropType = cropType;
    }
    
    @Override
    public Block getBlock() {
        return cropType.getCropBlock();
    }
    
    @Override
    public boolean matches(BlockState state) {
        return cropType.matches(state);
    }
    
    @Override
    public boolean onDiscovered(BlockPos pos, LevelAccessor level, BlockState state) {
        boolean isGrown = cropType.isFullyGrown(state);
        growthStates.put(pos, isGrown);
        return true;
    }
    
    @Override
    public void onRemoved(BlockPos pos, LevelAccessor level) {
        growthStates.remove(pos);
    }
    
    @Override
    public void onStateChanged(BlockPos pos, LevelAccessor level, BlockState newState) {
        if (matches(newState)) {
            growthStates.put(pos, cropType.isFullyGrown(newState));
        } else {
            growthStates.remove(pos);
        }
    }
    
    public boolean isFullyGrown(BlockPos pos) {
        return growthStates.getOrDefault(pos, false);
    }
    
    public void consumeCrop(BlockPos pos, LevelAccessor level) {
        if (level.getBlockState(pos).getBlock() instanceof CropBlock cropBlock) {
            // First remove the old state from tracking
            onRemoved(pos, level);
            
            // Reset the crop to age 0 (just planted)
            BlockState newState = cropBlock.getStateForAge(0);
            level.setBlock(pos, newState, 3);
            
            // Add the new state to tracking
            onDiscovered(pos, level, newState);
            
            // Notify the cache about the state change
            AutoBreed.getBlockTracker().getBlockCache().onBlockChanged(pos, level, newState);
        }
    }
    
    public CropType getCropType() {
        return cropType;
    }
} 