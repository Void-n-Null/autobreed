package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.tracking.TrackedHayBale;
import net.voidnull.autobreed.AutoBreedConfig;
import net.voidnull.autobreed.AutoBreed;

public class TargetHayBlockGoal extends AbstractTargetGoal<BlockPos> {
    private final TrackedHayBale hayTracker;

    public TargetHayBlockGoal(Animal animal, TrackedHayBale hayTracker) {
        this(animal, 1.0D, hayTracker);
    }

    public TargetHayBlockGoal(Animal animal, double speedModifier, TrackedHayBale hayTracker) {
        super(animal, speedModifier);
        this.hayTracker = hayTracker;
    }
    
    public TrackedHayBale getHayTracker() {
        return hayTracker;
    }

    @Override
    protected boolean isValidTarget(BlockPos target) {
        boolean matches = hayTracker.matches(animal.level().getBlockState(target));
        boolean canEat = hayTracker.canBeEaten(target);
        return matches && canEat;
    }

    @Override
    protected BlockPos findTarget() {
        BlockPos animalPos = animal.blockPosition();
        BlockPos target = AutoBreed.getBlockTracker().getBlockCache()
            .findNearest(animalPos, AutoBreedConfig.SEARCH_RADIUS.get(), hayTracker);
            
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
        return targetEntity != null && isValidTarget(targetEntity);
    }

    @Override
    public boolean canUse() {
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
        if(!animal.isFood(Items.WHEAT.getDefaultInstance())) {
            return false;
        }
        if(animal.isBaby()) {
            return false;
        }

        boolean canUse = super.canUse();
        return canUse;
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
}