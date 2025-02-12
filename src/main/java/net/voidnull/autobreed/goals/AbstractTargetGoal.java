package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public abstract class AbstractTargetGoal<T> extends Goal {
    protected final Animal animal;
    protected T targetEntity;
    protected final PathNavigation pathNav;
    protected final double speedModifier;
    protected static final double BASE_TARGET_DISTANCE = 1.5D;
    protected int timeToRecalcPath;
    protected boolean isRunning;

    protected AbstractTargetGoal(Animal animal, double speedModifier) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.pathNav = animal.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    protected abstract boolean isValidTarget(T target);
    protected abstract T findTarget();
    protected abstract void updatePathToTarget();
    protected abstract void lookAtTarget();
    protected abstract boolean isTargetValid();
    protected abstract Vec3 getTargetPos(T target);

    protected double getDesiredTargetDistance() {
        // Default implementation - can be overridden by subclasses
        return Math.max(BASE_TARGET_DISTANCE, animal.getBbWidth() + 0.5D);
    }

    protected boolean canMoveToTarget() {
        return !animal.isLeashed() && 
               animal.onGround() &&
               targetEntity != null &&
               isValidTarget(targetEntity);
    }

    protected double getDistanceToTarget() {
        if (targetEntity == null) return Double.MAX_VALUE;
        
        Vec3 entityPos = animal.position();
        Vec3 targetPos = getTargetPos(targetEntity);
        
        double dx = entityPos.x - targetPos.x;
        double dy = entityPos.y - targetPos.y;
        double dz = entityPos.z - targetPos.z;
        
        return Math.sqrt(dx * dx + dz * dz + dy * dy);
    }

    @Override
    public boolean canUse() {
        if (targetEntity != null && isTargetValid() && canMoveToTarget()) {
            return true;
        }
        
        T newTarget = findTarget();
        if (newTarget == null) return false;
        
        targetEntity = newTarget;
        return canMoveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return isRunning && 
               targetEntity != null && 
               isTargetValid() && 
               canMoveToTarget();
    }

    @Override
    public void start() {
        this.isRunning = true;
        this.timeToRecalcPath = 0;
        updatePathToTarget();
    }

    @Override
    public void stop() {
        this.isRunning = false;
        this.targetEntity = null;
        this.pathNav.stop();
    }

    @Override
    public void tick() {
        if (targetEntity == null || !isTargetValid()) {
            stop();
            return;
        }

        lookAtTarget();

        // Update path periodically
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (this.pathNav.isDone()) {
                updatePathToTarget();
            }
        }
    }

    public T getTarget() {
        return targetEntity;
    }

    public boolean isCloseEnoughToTarget() {
        return getDistanceToTarget() <= getDesiredTargetDistance();
    }
} 