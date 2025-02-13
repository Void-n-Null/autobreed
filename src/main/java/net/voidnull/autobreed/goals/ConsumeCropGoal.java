package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.BlockPos;
import net.voidnull.autobreed.tracking.TrackedCrop;
import net.voidnull.autobreed.AutoBreedConfig;

public class ConsumeCropGoal extends AbstractConsumeGoal<BlockPos, TargetCropGoal> {
    private final TrackedCrop cropTracker;

    public ConsumeCropGoal(Animal animal, TargetCropGoal targetGoal) {
        super(animal, targetGoal);
        this.cropTracker = targetGoal.getCropTracker();
    }

    @Override
    protected int getEatingCooldownTicks() {
        return AutoBreedConfig.FOOD_EATING_COOLDOWN_TICKS.get();
    }

    @Override
    protected void consumeTarget() {
        cropTracker.consumeCrop(targetResource, animal.level());
    }

    @Override
    protected boolean isTargetValid(BlockPos target) {
        return target != null && cropTracker.matches(animal.level().getBlockState(target));
    }

    @Override
    protected boolean canConsumeTarget(BlockPos target) {
        // Check if we're actually next to the crop
        BlockPos animalPos = animal.blockPosition();
        return isNextTo(animalPos, target);
    }

    private boolean isNextTo(BlockPos animalPos, BlockPos cropPos) {
        // Check all adjacent positions
        return animalPos.equals(cropPos.north()) ||
               animalPos.equals(cropPos.south()) ||
               animalPos.equals(cropPos.east()) ||
               animalPos.equals(cropPos.west());
    }
} 