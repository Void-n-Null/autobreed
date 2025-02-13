package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.tracking.TrackedHayBale;
import net.voidnull.autobreed.AutoBreedConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.voidnull.autobreed.AutoBreed;

public class TargetHayBlockGoal extends AbstractTargetGoal<BlockPos> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final TrackedHayBale hayTracker;

    public TargetHayBlockGoal(Animal animal, TrackedHayBale hayTracker) {
        this(animal, 1.0D, hayTracker);
    }

    public TargetHayBlockGoal(Animal animal, double speedModifier, TrackedHayBale hayTracker) {
        super(animal, speedModifier);
        this.hayTracker = hayTracker;
        LOGGER.debug("Created TargetHayBlockGoal for {}", 
            animal.getType().getDescription().getString());
    }
    
    public TrackedHayBale getHayTracker() {
        return hayTracker;
    }

    @Override
    protected boolean isValidTarget(BlockPos target) {
        boolean matches = hayTracker.matches(animal.level().getBlockState(target));
        boolean canEat = hayTracker.canBeEaten(target);
        LOGGER.debug("Checking hay bale at {} for {}: matches={}, canEat={}", 
            target, animal.getType().getDescription().getString(), matches, canEat);
        return matches && canEat;
    }

    @Override
    protected BlockPos findTarget() {
        LOGGER.debug("Looking for hay bales near {}", 
            animal.getType().getDescription().getString());
            
        BlockPos animalPos = animal.blockPosition();
        BlockPos target = AutoBreed.getBlockTracker().getBlockCache()
            .findNearest(animalPos, AutoBreedConfig.SEARCH_RADIUS.get(), hayTracker);
            
        if (target != null) {
            LOGGER.info("Found hay bale at {} for {}", target,
                animal.getType().getDescription().getString());
        } else {
            LOGGER.debug("No hay bales found for {}", 
                animal.getType().getDescription().getString());
        }
        
        return target;
    }

    @Override
    protected void updatePathToTarget() {
        if (targetEntity != null) {
            LOGGER.debug("{} moving to hay bale at {}", 
                animal.getType().getDescription().getString(), targetEntity);
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
        LOGGER.debug("Checking if hay bale target is valid for {}: {}", 
            animal.getType().getDescription().getString(), valid);
        return valid;
    }

    @Override
    public boolean canUse() {
        if(animal.isInLove()) {
            LOGGER.debug("{} can't use hay bale: in love", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(!animal.canFallInLove()) {
            LOGGER.debug("{} can't use hay bale: can't fall in love", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(animal.canBreed()) {
            LOGGER.debug("{} can't use hay bale: can breed", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(animal.getAge() != 0) {
            LOGGER.debug("{} can't use hay bale: wrong age", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(!animal.isFood(Items.WHEAT.getDefaultInstance())) {
            LOGGER.debug("{} can't use hay bale: doesn't eat wheat", 
                animal.getType().getDescription().getString());
            return false;
        }
        if(animal.isBaby()) {
            LOGGER.debug("{} can't use hay bale: is baby", 
                animal.getType().getDescription().getString());
            return false;
        }

        boolean canUse = super.canUse();
        if (canUse) {
            LOGGER.info("{} can use hay bale target", 
                animal.getType().getDescription().getString());
        }
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