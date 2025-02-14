package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.tracking.TrackedCrop;
import net.voidnull.autobreed.AutoBreedConfig;
import net.voidnull.autobreed.AutoBreed;

public class TargetCropGoal extends AbstractTargetGoal<BlockPos> {
    private final TrackedCrop cropTracker;
    private BlockPos lastFailedTarget;
    private int retryTargetTicks;

    public TargetCropGoal(Animal animal, TrackedCrop cropTracker) {
        this(animal, 1.0D, cropTracker);
    }

    public TargetCropGoal(Animal animal, double speedModifier, TrackedCrop cropTracker) {
        super(animal, speedModifier);
        this.cropTracker = cropTracker;
    }
    
    public TrackedCrop getCropTracker() {
        return cropTracker;
    }

    @Override
    protected boolean isValidTarget(BlockPos target) {
        if (target == null || target.equals(lastFailedTarget)) {
            return false;
        }
        boolean matches = cropTracker.matches(animal.level().getBlockState(target));
        boolean grown = cropTracker.isFullyGrown(target);
        return matches && grown;
    }

    @Override
    protected BlockPos findTarget() {
        BlockPos animalPos = animal.blockPosition();
        BlockPos target = AutoBreed.getBlockTracker().getBlockCache()
            .findNearest(animalPos, AutoBreedConfig.SEARCH_RADIUS.get(), cropTracker);
        
        // If we found the same target that recently failed, ignore it for a while
        if (target != null && target.equals(lastFailedTarget)) {
            if (retryTargetTicks > 0) {
                return null;
            }
            // Reset failed target after cooldown
            lastFailedTarget = null;
        }
        
        return target;
    }

    @Override
    protected void updatePathToTarget() {
        if (targetEntity != null) {
            this.pathNav.moveTo(targetEntity.getX() + 0.5, targetEntity.getY(), targetEntity.getZ() + 0.5, this.speedModifier);
        }
    }

    @Override
    protected void lookAtTarget() {
        this.animal.getLookControl().setLookAt(
            targetEntity.getX() + 0.5,
            targetEntity.getY(),
            targetEntity.getZ() + 0.5,
            (float)(this.animal.getMaxHeadXRot() + 20),
            (float)this.animal.getMaxHeadXRot()
        );
    }

    @Override
    protected boolean isTargetValid() {
        boolean valid = targetEntity != null && isValidTarget(targetEntity);
        if (!valid && targetEntity != null) {
            // Mark this target as failed so we don't immediately try it again
            lastFailedTarget = targetEntity;
            retryTargetTicks = 100; // Wait 5 seconds before retrying this target
        }
        return valid;
    }

    @Override
    public boolean canUse() {
        // First find a target
        BlockPos newTarget = findTarget();
        if (newTarget == null) {
            return false;
        }

        // For babies, we only need to check if they can move to the target
        if(animal.isBaby()) {
            targetEntity = newTarget;
            return canMoveToTarget();
        }

        // For adults, check breeding conditions
        if(animal.isInLove()) {
            return false;
        }
        if(!animal.canFallInLove()) {
            return false;
        }
        if(animal.canBreed()) {
            return false;
        }
        if(animal.getAge() != 0) {
            return false;
        }
        if(!animal.isFood(cropTracker.getCropType().getCropItem().getDefaultInstance())) {
            return false;
        }

        targetEntity = newTarget;
        return canMoveToTarget();
    }

    @Override
    public void tick() {
        super.tick();
        if (retryTargetTicks > 0) {
            retryTargetTicks--;
        }
    }

    @Override
    protected Vec3 getTargetPos(BlockPos target) {
        return new Vec3(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
    }

    @Override
    protected double getDesiredTargetDistance() {
        // Use a more generous distance check for larger entities
        return Math.max(BASE_TARGET_DISTANCE, animal.getBbWidth() + 0.75D);
    }

    public BlockPos getTargetPos() {
        return getTarget();
    }
    
    public void clearTarget() {
        targetEntity = null;
    }
} 