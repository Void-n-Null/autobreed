package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.voidnull.autobreed.AutoBreedConfig;

public class ConsumeFoodGoal extends AbstractConsumeGoal<ItemEntity, TargetFoodGoal> {

    public ConsumeFoodGoal(Animal animal, TargetFoodGoal targetGoal) {
        super(animal, targetGoal);
    }

    @Override
    protected int getEatingCooldownTicks() {
        return AutoBreedConfig.FOOD_EATING_COOLDOWN_TICKS.get();
    }

    @Override
    protected void consumeTarget() {
        ItemStack foodStack = targetResource.getItem();
        foodStack.shrink(1);
        if (foodStack.isEmpty()) {
            targetResource.discard();
        }
    }

    @Override
    protected boolean isTargetValid(ItemEntity target) {
        return target != null && target.isAlive();
    }

    @Override
    protected boolean canConsumeTarget(ItemEntity target) {
        return true; // Food items can always be consumed if we got this far
    }
} 