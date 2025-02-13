package net.voidnull.autobreed.tracking;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

/**
 * Represents a block type that can be tracked by the caching system.
 * This interface defines the contract for blocks that want to participate
 * in the chunk-based caching system.
 */
public interface TrackedBlock {
    /**
     * Get the block type being tracked
     */
    Block getBlock();

    /**
     * Check if a given block state matches this tracked block type
     */
    boolean matches(BlockState state);

    /**
     * Called when this block is discovered in the world
     * @return true if the block should be tracked
     */
    boolean onDiscovered(BlockPos pos, LevelAccessor level, BlockState state);

    /**
     * Called when a tracked block is removed from the world
     */
    void onRemoved(BlockPos pos, LevelAccessor level);

    /**
     * Called when a tracked block's state changes
     */
    void onStateChanged(BlockPos pos, LevelAccessor level, BlockState newState);
} 