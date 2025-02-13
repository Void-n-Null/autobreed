package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.tracking.TrackedCrop;
import net.voidnull.autobreed.AutoBreedConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.voidnull.autobreed.AutoBreed;

public class TargetCropGoal extends AbstractTargetGoal<BlockPos> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final TrackedCrop cropTracker;

    public TargetCropGoal(Animal animal, TrackedCrop cropTracker) {
        this(animal, 1.0D, cropTracker);
    }

    public TargetCropGoal(Animal animal, double speedModifier, TrackedCrop cropTracker) {
        super(animal, speedModifier);
        this.cropTracker = cropTracker;
        LOGGER.debug("Created TargetCropGoal for {} to target {}", 
            animal.getType().getDescription().getString(),
            cropTracker.getCropType().name());
    }
    
    public TrackedCrop getCropTracker() {
        return cropTracker;
    }

    @Override
    protected boolean isValidTarget(BlockPos target) {
        boolean matches = cropTracker.matches(animal.level().getBlockState(target));
        boolean grown = cropTracker.isFullyGrown(target);
        LOGGER.debug("Checking target {} for {}: matches={}, grown={}", 
            target, animal.getType().getDescription().getString(), matches, grown);
        return matches && grown;
    }

    @Override
    protected BlockPos findTarget() {
        LOGGER.debug("Looking for {} crops near {}", 
            cropTracker.getCropType().name(),
            animal.getType().getDescription().getString());
            
        BlockPos animalPos = animal.blockPosition();
        BlockPos target = AutoBreed.getBlockTracker().getBlockCache()
            .findNearest(animalPos, AutoBreedConfig.SEARCH_RADIUS.get(), cropTracker);
            
        if (target != null) {
            LOGGER.info("Found {} crop at {} for {}", 
                cropTracker.getCropType().name(), target,
                animal.getType().getDescription().getString());
        } else {
            LOGGER.debug("No {} crops found for {}", 
                cropTracker.getCropType().name(),
                animal.getType().getDescription().getString());
        }
        
        return target;
    }

    @Override
    protected void updatePathToTarget() {
        if (targetEntity != null) {
            LOGGER.debug("{} moving to {} at {}", 
                animal.getType().getDescription().getString(),
                cropTracker.getCropType().name(), targetEntity);
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
        LOGGER.debug("Checking if target is valid for {}: {}", 
            animal.getType().getDescription().getString(), valid);
        return valid;
    }

    @Override
    public boolean canUse() {
        // First find a target
        if (targetEntity != null && isTargetValid() && canMoveToTarget()) {
            LOGGER.debug("{} can use existing target", 
                animal.getType().getDescription().getString());
            return true;
        }
        
        BlockPos newTarget = findTarget();
        if (newTarget == null) {
            LOGGER.debug("{} found no target", 
                animal.getType().getDescription().getString());
            return false;
        }

        if(animal.isBaby()) {
            LOGGER.debug("Baby {} can use target", 
                animal.getType().getDescription().getString());
            targetEntity = newTarget;
            return canMoveToTarget();
        }

        // Then check breeding conditions
        if(animal.isInLove()) {
            LOGGER.debug("{} can't use target: in love", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(!animal.canFallInLove()) {
            LOGGER.debug("{} can't use target: can't fall in love", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(animal.canBreed()) {
            LOGGER.debug("{} can't use target: can breed", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(animal.getAge() != 0) {
            LOGGER.debug("{} can't use target: wrong age", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(!animal.isFood(cropTracker.getCropType().getCropItem().getDefaultInstance())) {
            LOGGER.debug("{} can't use target: doesn't eat {}", 
                animal.getType().getDescription().getString(),
                cropTracker.getCropType().name());
            return false;
        }

        LOGGER.info("Adult {} can use target at {}", 
            animal.getType().getDescription().getString(), newTarget);
        targetEntity = newTarget;
        return canMoveToTarget();
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