package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.voidnull.autobreed.WheatCropDataManager;
import net.voidnull.autobreed.AutoBreedConfig;

public class ConsumeWheatCropGoal extends AbstractConsumeGoal<BlockPos, TargetWheatCropGoal> {

    public ConsumeWheatCropGoal(Animal animal, TargetWheatCropGoal targetGoal) {
        super(animal, targetGoal);
    }

    @Override
    protected int getEatingCooldownTicks() {
        return AutoBreedConfig.FOOD_EATING_COOLDOWN_TICKS.get(); // Use same cooldown as food items
    }

    @Override
    protected void consumeTarget() {
        WheatCropDataManager.consumeWheatCrop(targetResource, animal.level());
    }

    @Override
    protected boolean isTargetValid(BlockPos target) {
        return target != null && animal.level().getBlockState(target).is(Blocks.WHEAT);
    }

    @Override
    protected boolean canConsumeTarget(BlockPos target) {
        // Check if we're actually next to the wheat crop
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