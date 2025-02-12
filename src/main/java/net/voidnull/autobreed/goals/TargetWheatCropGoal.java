package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.WheatCropCache;
import net.voidnull.autobreed.AutoBreedConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class TargetWheatCropGoal extends AbstractTargetGoal<BlockPos> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public TargetWheatCropGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetWheatCropGoal(Animal animal, double speedModifier) {
        super(animal, speedModifier);
    }

    @Override
    protected boolean isValidTarget(BlockPos target) {
        if (!animal.level().getBlockState(target).is(Blocks.WHEAT)) {
            return false;
        }
        
        // Check if the crop is fully grown
        CropBlock cropBlock = (CropBlock) Blocks.WHEAT;
        boolean isMaxAge = cropBlock.isMaxAge(animal.level().getBlockState(target));
        
        if (isMaxAge) {
            LOGGER.debug("Found valid fully grown wheat target at {}", target);
        }
        
        return isMaxAge;
    }

    @Override
    protected BlockPos findTarget() {
        int searchRadius = AutoBreedConfig.SEARCH_RADIUS.get();
        BlockPos target = WheatCropCache.findNearestFullyGrownWheat(
            animal.blockPosition(),
            searchRadius
        );
        
        if (target != null) {
            LOGGER.debug("Found nearest fully grown wheat crop at {} for {}", target, animal);
        } else {
            LOGGER.debug("No fully grown wheat crops found nearby for {}", animal);
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
        return targetEntity != null && isValidTarget(targetEntity);
    }

    @Override
    public boolean canUse() {
        // First find a target
        if (targetEntity != null && isTargetValid() && canMoveToTarget()) {
            return true;
        }
        
        BlockPos newTarget = findTarget();
        if (newTarget == null) {
            LOGGER.debug("No wheat crops found nearby for {}", animal);
            return false;
        }
        
        

        if(animal.isBaby()) {
            targetEntity = newTarget;
            return canMoveToTarget();
        }

        // Then check breeding conditions
        if(animal.isInLove()) {
            LOGGER.debug("{} can't target wheat: in love", animal);
            return false;
        }
        if(!animal.canFallInLove()) {
            LOGGER.debug("{} can't target wheat: can't fall in love", animal);
            return false;
        }
        if(animal.canBreed()) {
            LOGGER.debug("{} can't target wheat: can breed", animal);
            return false;
        }
        if(animal.getAge() != 0) {
            LOGGER.debug("{} can't target wheat: wrong age", animal);
            return false;
        }
        if(!animal.isFood(Items.WHEAT.getDefaultInstance())) {
            LOGGER.debug("{} can't target wheat: doesn't eat wheat", animal);
            return false;
        }

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