package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.EnumSet;

public class ConsumeFoodGoal extends Goal {
    private final Animal animal;
    private final TargetFoodGoal targetGoal;
    private static final int EATING_COOLDOWN_TICKS = 20;
    private static final int BABY_GROWTH_TICKS = 200; // 10 seconds worth of growth
    private int cooldown = 0;
    private ItemEntity targetFood = null;

    public ConsumeFoodGoal(Animal animal, TargetFoodGoal targetGoal) {
        this.animal = animal;
        this.targetGoal = targetGoal;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {


        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (!targetGoal.isCloseEnoughToTarget()) {
            return false;
        }
        
        ItemEntity food = targetGoal.getTargetFood();
        if (food == null || !food.isAlive()) {
            return false;
        }

        if (animal.isBaby()) {
            targetFood = food;
            return true;
        }

        if(animal.isInLove()) return false;
        if(!animal.canFallInLove()) return false;
        if(animal.canBreed()) return false;
        if(animal.getAge() != 0) return false;

        targetFood = food;
        return true;
    }

    @Override
    public void start() {
        if (targetFood == null) {
            return;
        }

        if (animal.level().isClientSide()) {
            return;
        }

        ItemStack foodStack = targetFood.getItem();

        animal.level().playSound(null, animal, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (animal.isBaby()) {
            foodStack.shrink(1);
            cooldown = EATING_COOLDOWN_TICKS;
            
            // Age up by a small amount (10 seconds worth of growth)
            animal.ageUp(BABY_GROWTH_TICKS);
        } else if (animal.canFallInLove()) {
            foodStack.shrink(1);
            cooldown = EATING_COOLDOWN_TICKS;
            
            animal.setInLove(null);
        } else {
        }

        if (foodStack.isEmpty()) {
            targetFood.discard();
        }
        targetFood = null; // Clear the cached target food after consumption
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
} 