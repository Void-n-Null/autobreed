package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.AutoBreedConfig;

public abstract class AbstractEntityTargetGoal<T extends Entity> extends AbstractTargetGoal<T> {

    protected AbstractEntityTargetGoal(Animal animal, double speedModifier) {
        super(animal, speedModifier);
    }

    @Override
    protected void updatePathToTarget() {
        if (targetEntity != null) {
            this.pathNav.moveTo(targetEntity, this.speedModifier);
        }
    }

    @Override
    protected void lookAtTarget() {
        this.animal.getLookControl().setLookAt(
            targetEntity,
            (float)(this.animal.getMaxHeadXRot() + 20),
            (float)this.animal.getMaxHeadXRot()
        );
    }

    @Override
    protected boolean isTargetValid() {
        return targetEntity != null && targetEntity.isAlive();
    }

    @Override
    protected Vec3 getTargetPos(T target) {
        return target.position();
    }

    protected abstract Class<T> getTargetClass();
    protected abstract boolean isValidFoodSource(T entity);

    @Override
    protected T findTarget() {
        return this.animal.level().getEntitiesOfClass(getTargetClass(),
            this.animal.getBoundingBox().inflate(
                AutoBreedConfig.SEARCH_RADIUS.get(),
                AutoBreedConfig.SEARCH_VERTICAL_RADIUS.get(),
                AutoBreedConfig.SEARCH_RADIUS.get()))
            .stream()
            .filter(this::isValidFoodSource)
            .min((a, b) -> Double.compare(
                animal.distanceToSqr(a),
                animal.distanceToSqr(b)))
            .orElse(null);
    }
} 