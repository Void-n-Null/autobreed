package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class TargetFoodGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Animal animal;
    private ItemEntity targetFood;
    private final PathNavigation pathNav;
    private final double speedModifier;
    private static final double TARGET_DISTANCE = 1.0D;
    private int timeToRecalcPath;
    private boolean isRunning;

    public TargetFoodGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetFoodGoal(Animal animal, double speedModifier) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.pathNav = animal.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean canMoveToTarget() {
        return !animal.isLeashed() && 
               animal.onGround() &&
               targetFood != null &&
               animal.isFood(targetFood.getItem());
    }

    private ItemEntity findFood() {
        List<ItemEntity> list = this.animal.level().getEntitiesOfClass(ItemEntity.class,
            this.animal.getBoundingBox().inflate(8.0D, 4.0D, 8.0D));
        
        if (list.isEmpty()) return null;

        return list.stream()
            .filter(itemEntity -> animal.isFood(itemEntity.getItem()))
            .min((a, b) -> Double.compare(
                animal.distanceToSqr(a),
                animal.distanceToSqr(b)))
            .orElse(null);
    }

    @Override
    public boolean canUse() {
        if (targetFood != null && targetFood.isAlive() && canMoveToTarget()) {
            LOGGER.debug("Continuing to target existing food for {}", animal);
            return true;
        }
        
        ItemEntity newTarget = findFood();
        if (newTarget == null) return false;
        
        targetFood = newTarget;
        LOGGER.debug("Found new food target {} for {}", targetFood, animal);
        return canMoveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return isRunning && 
               targetFood != null && 
               targetFood.isAlive() && 
               canMoveToTarget();
    }

    @Override
    public void start() {
        LOGGER.debug("Starting movement to food {} for {}", targetFood, animal);
        this.isRunning = true;
        this.timeToRecalcPath = 0;
        updatePath();
    }

    @Override
    public void stop() {
        LOGGER.debug("Stopping movement for {}", animal);
        this.isRunning = false;
        this.targetFood = null;
        this.pathNav.stop();
    }

    private void updatePath() {
        if (targetFood != null) {
            this.pathNav.moveTo(targetFood, this.speedModifier);
        }
    }

    @Override
    public void tick() {
        if (targetFood == null || !targetFood.isAlive()) {
            stop();
            return;
        }

        // Look at the food
        this.animal.getLookControl().setLookAt(
            targetFood,
            (float)(this.animal.getMaxHeadXRot() + 20),
            (float)this.animal.getMaxHeadXRot()
        );

        // Update path periodically
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (this.pathNav.isDone()) {
                LOGGER.debug("Updating path to food {} for {}", targetFood, animal);
                updatePath();
            }
        }
    }

    public ItemEntity getTargetFood() {
        return targetFood;
    }

    public boolean isCloseEnoughToTarget() {
        if (targetFood == null) return false;
        return animal.distanceTo(targetFood) <= TARGET_DISTANCE;
    }
} 