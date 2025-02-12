package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class ConsumeFoodGoal extends AbstractConsumeGoal<ItemEntity, TargetFoodGoal> {
    private static final int EATING_COOLDOWN_TICKS = 20;

    public ConsumeFoodGoal(Animal animal, TargetFoodGoal targetGoal) {
        super(animal, targetGoal);
    }

    @Override
    protected int getEatingCooldownTicks() {
        return EATING_COOLDOWN_TICKS;
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