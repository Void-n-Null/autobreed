package net.voidnull.autobreed.tracking;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackedHayBale implements TrackedBlock {
    private static final int MAX_EATEN_COUNT = 10;
    private final Map<BlockPos, Integer> eatenCounts = new ConcurrentHashMap<>();
    
    @Override
    public Block getBlock() {
        return Blocks.HAY_BLOCK;
    }
    
    @Override
    public boolean matches(BlockState state) {
        return state.is(Blocks.HAY_BLOCK);
    }
    
    @Override
    public boolean onDiscovered(BlockPos pos, LevelAccessor level, BlockState state) {
        eatenCounts.putIfAbsent(pos, 0);
        return true;
    }
    
    @Override
    public void onRemoved(BlockPos pos, LevelAccessor level) {
        eatenCounts.remove(pos);
    }
    
    @Override
    public void onStateChanged(BlockPos pos, LevelAccessor level, BlockState newState) {
        if (!matches(newState)) {
            eatenCounts.remove(pos);
        }
    }
    
    public boolean canBeEaten(BlockPos pos) {
        return eatenCounts.getOrDefault(pos, 0) < MAX_EATEN_COUNT;
    }
    
    public void consumeHayBale(BlockPos pos) {
        eatenCounts.compute(pos, (k, v) -> v == null ? 1 : v + 1);
    }
    
    public int getEatenCount(BlockPos pos) {
        return eatenCounts.getOrDefault(pos, 0);
    }
} 