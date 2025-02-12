package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;
import java.util.List;

public class TargetFoodGoal extends AbstractEntityTargetGoal<ItemEntity> {

    public TargetFoodGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetFoodGoal(Animal animal, double speedModifier) {
        super(animal, speedModifier);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    protected Class<ItemEntity> getTargetClass() {
        return ItemEntity.class;
    }

    @Override
    protected boolean isValidFoodSource(ItemEntity entity) {
        return animal.isFood(entity.getItem());
    }

    @Override
    protected boolean isValidTarget(ItemEntity target) {
        return isValidFoodSource(target);
    }

    @Override
    protected ItemEntity findTarget() {
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
    protected Vec3 getTargetPos(ItemEntity target) {
        return target.position();
    }

    public ItemEntity getTargetFood() {
        return getTarget();
    }
}