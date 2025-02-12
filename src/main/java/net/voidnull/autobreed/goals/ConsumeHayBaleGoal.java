package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.voidnull.autobreed.HayBaleDataManager;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ConsumeHayBaleGoal extends AbstractConsumeGoal<BlockPos, TargetHayBlockGoal> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EATING_COOLDOWN_TICKS = 40;

    public ConsumeHayBaleGoal(Animal animal, TargetHayBlockGoal targetGoal) {
        super(animal, targetGoal);
    }

    @Override
    protected int getEatingCooldownTicks() {
        return EATING_COOLDOWN_TICKS;
    }

    @Override
    protected void consumeTarget() {
        HayBaleDataManager.decrementEatenCount(targetResource, animal.level());
    }

    @Override
    protected boolean isTargetValid(BlockPos target) {
        return target != null && animal.level().getBlockState(target).is(Blocks.HAY_BLOCK);
    }

    @Override
    protected boolean canConsumeTarget(BlockPos target) {
        // Check if we're actually on top of or adjacent to the hay block
        BlockPos animalPos = animal.blockPosition();
        return isNextToOrAbove(animalPos, target);
    }

    private boolean isNextToOrAbove(BlockPos animalPos, BlockPos hayPos) {
        if (animalPos.equals(hayPos.above())) {
            return true; // On top
        }
        
        // Check all adjacent positions
        return animalPos.equals(hayPos.north()) ||
               animalPos.equals(hayPos.south()) ||
               animalPos.equals(hayPos.east()) ||
               animalPos.equals(hayPos.west());
    }
}