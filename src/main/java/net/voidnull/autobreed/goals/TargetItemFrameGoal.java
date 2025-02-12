package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class TargetItemFrameGoal extends AbstractTargetGoal<ItemFrame> {

    public TargetItemFrameGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetItemFrameGoal(Animal animal, double speedModifier) {
        super(animal, speedModifier);
    }

    @Override
    protected boolean isValidTarget(ItemFrame target) {
        return animal.isFood(target.getItem());
    }

    @Override
    protected ItemFrame findTarget() {
        List<ItemFrame> frames = this.animal.level().getEntitiesOfClass(ItemFrame.class,
            this.animal.getBoundingBox().inflate(8.0D, 4.0D, 8.0D));
        
        return frames.stream()
            .filter(frame -> animal.isFood(frame.getItem()))
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
    protected Vec3 getTargetPos(ItemFrame target) {
        return target.position();
    }

    @Override
    protected double getDesiredTargetDistance() {
        // Item frames need a bit more distance since they're on walls
        return BASE_TARGET_DISTANCE + 0.5D;
    }

    public ItemFrame getTargetItemFrame() {
        return getTarget();
    }
} 