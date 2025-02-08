package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class EatDroppedFoodGoal extends Goal {
    private final Animal animal;
    private ItemEntity targetFood;
    private int eatingCooldown = 0;
    private static final int EATING_COOLDOWN_TICKS = 30; // 1.5 seconds between eating
    private static final double EATING_DISTANCE = 1.5D; // Increased eating distance
    private static final double EATING_DISTANCE_SQ = EATING_DISTANCE * EATING_DISTANCE;
    private final PathNavigation pathNav;
    private final double speedModifier;
    private int timeToRecalcPath;
    private boolean isRunning;

    public EatDroppedFoodGoal(Animal animal) {
        this(animal, 0.7D);
    }

    public EatDroppedFoodGoal(Animal animal, double speedModifier) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.pathNav = animal.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    // Movement-related checks
    private boolean canMoveToFood() {
        return !animal.isLeashed() && animal.isFood(targetFood.getItem());
    }

    // Consumption-related checks
    private boolean canConsumeFood() {
        if (animal.isBaby()) {
            return true;
        }
        return !animal.isInLove() && animal.canFallInLove();
    }

    private ItemEntity findFood() {
        List<ItemEntity> list = this.animal.level().getEntitiesOfClass(ItemEntity.class, 
            this.animal.getBoundingBox().inflate(8.0D, 4.0D, 8.0D));
        
        if (list.isEmpty()) return null;

        return list.stream()
            .filter(itemEntity -> animal.isFood(itemEntity.getItem()))
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean canUse() {
        if (eatingCooldown > 0) return false;

        ItemEntity newTarget = findFood();
        if (newTarget == null) return false;
        
        targetFood = newTarget;
        return canMoveToFood();
    }

    @Override
    public void start() {
        this.isRunning = true;
        this.timeToRecalcPath = 0;
        this.eatingCooldown = 0;
    }

    @Override
    public void stop() {
        this.targetFood = null;
        this.pathNav.stop();
        this.isRunning = false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.isRunning && 
               targetFood != null && 
               targetFood.isAlive() && 
               canMoveToFood();
    }

    private void handleMovement() {
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            double distSq = this.animal.distanceToSqr(targetFood);
            
            if (distSq > EATING_DISTANCE_SQ) {
                this.pathNav.moveTo(targetFood.getX(), targetFood.getY(), targetFood.getZ(), this.speedModifier);
            } else if (!this.pathNav.isDone()) {
                // Stop moving if we're close enough
                this.pathNav.stop();
            }
        }
    }

    private void handleEating() {
        if (animal.level().isClientSide()) return;
        
        if (this.animal.distanceToSqr(targetFood) <= EATING_DISTANCE_SQ && canConsumeFood()) {
            ItemStack foodStack = targetFood.getItem();
            foodStack.shrink(1);
            eatingCooldown = EATING_COOLDOWN_TICKS;

            if (animal.isBaby()) {
                int growthAmount = -2400;
                animal.ageUp(growthAmount);
            } else {
                animal.setInLove(null);
            }

            if (foodStack.isEmpty()) {
                targetFood.discard();
                stop();
            }
        }
    }

    @Override
    public void tick() {
        if (targetFood == null || !targetFood.isAlive()) {
            stop();
            return;
        }

        if (eatingCooldown > 0) {
            eatingCooldown--;
            return;
        }

        // Look at the food
        this.animal.getLookControl().setLookAt(
            targetFood,
            (float)(this.animal.getMaxHeadXRot() + 20),
            (float)this.animal.getMaxHeadXRot()
        );

        handleMovement();
        handleEating();
    }
} 