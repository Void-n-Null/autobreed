package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import java.util.EnumSet;
import java.util.List;

public class TargetFoodGoal extends Goal {
    private final Animal animal;
    private ItemEntity targetEntity;
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
               targetEntity != null &&
               isValidFoodSource(targetEntity);
    }

    private boolean isValidFoodSource(ItemEntity itemEntity) {
        return animal.isFood(itemEntity.getItem());
    }

    private ItemEntity findFood() {
        List<ItemEntity> items = this.animal.level().getEntitiesOfClass(ItemEntity.class,
            this.animal.getBoundingBox().inflate(8.0D, 4.0D, 8.0D));
        
        return items.stream()
            .filter(itemEntity -> animal.isFood(itemEntity.getItem()))
            .min((a, b) -> Double.compare(
                animal.distanceToSqr(a),
                animal.distanceToSqr(b)))
            .orElse(null);
    }

    @Override
    public boolean canUse() {
        if (targetEntity != null && targetEntity.isAlive() && canMoveToTarget()) {
            return true;
        }
        
        ItemEntity newTarget = findFood();
        if (newTarget == null) return false;
        
        targetEntity = newTarget;
        return canMoveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return isRunning && 
               targetEntity != null && 
               targetEntity.isAlive() && 
               canMoveToTarget();
    }

    @Override
    public void start() {
        this.isRunning = true;
        this.timeToRecalcPath = 0;
        updatePath();
    }

    @Override
    public void stop() {
        this.isRunning = false;
        this.targetEntity = null;
        this.pathNav.stop();
    }

    private void updatePath() {
        if (targetEntity != null) {
            this.pathNav.moveTo(targetEntity, this.speedModifier);
        }
    }

    @Override
    public void tick() {
        if (targetEntity == null || !targetEntity.isAlive()) {
            stop();
            return;
        }

        // Look at the food source
        this.animal.getLookControl().setLookAt(
            targetEntity,
            (float)(this.animal.getMaxHeadXRot() + 20),
            (float)this.animal.getMaxHeadXRot()
        );

        // Update path periodically
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (this.pathNav.isDone()) {
                updatePath();
            }
        }
    }

    public ItemEntity getTargetFood() {
        return targetEntity;
    }

    public boolean isCloseEnoughToTarget() {
        if (targetEntity == null) return false;
        return animal.distanceTo(targetEntity) <= TARGET_DISTANCE;
    }
}