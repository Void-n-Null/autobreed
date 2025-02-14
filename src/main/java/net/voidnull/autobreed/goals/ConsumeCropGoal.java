package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.core.BlockPos;
import net.voidnull.autobreed.tracking.TrackedCrop;
import net.voidnull.autobreed.AutoBreedConfig;
import java.util.Map;
import java.util.WeakHashMap;

public class ConsumeCropGoal extends AbstractConsumeGoal<BlockPos, TargetCropGoal> {
    private final TrackedCrop cropTracker;
    // Use WeakHashMap to avoid memory leaks - animals will be garbage collected when they're removed
    private static final Map<Animal, Long> lastConsumedTicks = new WeakHashMap<>();

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
        lastConsumedTicks.put(animal, animal.level().getGameTime());
        // Force target goal to find a new target
        targetGoal.clearTarget();
    }

    @Override
    protected boolean isTargetValid(BlockPos target) {
        // Check if this animal recently consumed any crop
        Long lastConsumed = lastConsumedTicks.get(animal);
        if (lastConsumed != null && animal.level().getGameTime() - lastConsumed < getEatingCooldownTicks()) {
            return false;
        }
        return target != null && 
               cropTracker.matches(animal.level().getBlockState(target)) && 
               cropTracker.isFullyGrown(target);
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