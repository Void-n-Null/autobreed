package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.BlockPos;
import net.voidnull.autobreed.tracking.TrackedHayBale;

public class ConsumeHayBaleGoal extends AbstractConsumeGoal<BlockPos, TargetHayBlockGoal> {
    private final TrackedHayBale hayTracker;

    public ConsumeHayBaleGoal(Animal animal, TargetHayBlockGoal targetGoal) {
        super(animal, targetGoal);
        this.hayTracker = targetGoal.getHayTracker();
    }

    @Override
    protected int getEatingCooldownTicks() {
        return 0; // No cooldown needed since this is adult-only
    }

    @Override
    protected void consumeTarget() {
        hayTracker.consumeHayBale(targetResource);
    }

    @Override
    protected boolean isTargetValid(BlockPos target) {
        return target != null && hayTracker.matches(animal.level().getBlockState(target));
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