package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.EnumSet;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ConsumeFoodGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
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
            LOGGER.debug("Animal {} distance check failed", animal);
            return false;
        }
        
        ItemEntity food = targetGoal.getTargetFood();
        if (food == null || !food.isAlive()) {
            LOGGER.debug("Food null or not alive for {}", animal);
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
            LOGGER.debug("Target food null in start() for {}", animal);
            return;
        }

        if (animal.level().isClientSide()) {
            LOGGER.debug("Client side, skipping consumption for {}", animal);
            return;
        }

        LOGGER.debug("Starting consumption for {} at distance {}", 
            animal, animal.distanceTo(targetFood));

        ItemStack foodStack = targetFood.getItem();
        if (animal.isBaby()) {
            LOGGER.debug("Baby {} consuming {}", animal, foodStack);
            foodStack.shrink(1);
            cooldown = EATING_COOLDOWN_TICKS;
            
            // Age up by a small amount (10 seconds worth of growth)
            animal.ageUp(BABY_GROWTH_TICKS);
            LOGGER.debug("Baby {} aged up by {} ticks", animal, BABY_GROWTH_TICKS);
        } else if (animal.canFallInLove()) {
            LOGGER.debug("Adult {} consuming {}", animal, foodStack);
            foodStack.shrink(1);
            cooldown = EATING_COOLDOWN_TICKS;
            
            animal.setInLove(null);
            LOGGER.debug("Adult {} set to love mode", animal);
        } else {
            LOGGER.debug("Animal {} cannot consume (not baby and can't fall in love)", animal);
        }

        if (foodStack.isEmpty()) {
            targetFood.discard();
            LOGGER.debug("Food stack empty, discarded for {}", animal);
        }
        targetFood = null; // Clear the cached target food after consumption
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
} 