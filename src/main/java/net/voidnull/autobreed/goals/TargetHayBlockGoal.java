package net.voidnull.autobreed.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.voidnull.autobreed.HayBaleCache;
import net.voidnull.autobreed.AutoBreedConfig;
import java.util.EnumSet;

public class TargetHayBlockGoal extends Goal {
    private final Animal animal;
    private BlockPos targetPos;  // Using BlockPos for hay blocks
    private final PathNavigation pathNav;
    private final double speedModifier;
    private int timeToRecalcPath;
    private boolean isRunning;

    public TargetHayBlockGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetHayBlockGoal(Animal animal, double speedModifier) {
        this.animal = animal;
        this.speedModifier = speedModifier;
        this.pathNav = animal.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean canMoveToTarget() {
        return !animal.isLeashed() && 
               animal.onGround() &&
               targetPos != null &&
               animal.level().getBlockState(targetPos).is(Blocks.HAY_BLOCK);
    }

    private BlockPos findHayBlock() {
        // Use our cache system to find the nearest hay bale
        // The search radius is configured in AutoBreedConfig
        return HayBaleCache.findNearestHayBale(
            animal.blockPosition(),
            AutoBreedConfig.HAY_SEARCH_RADIUS.get()
        );
    }

    @Override
    public boolean canUse() {
        if(animal.isInLove()) return false;
        if(!animal.canFallInLove()) return false;
        if(animal.canBreed()) return false;
        if(animal.getAge() != 0) return false;
        if(!animal.isFood(Items.WHEAT.getDefaultInstance())) return false;
        if(animal.isBaby()) return false;

        if (targetPos != null && canMoveToTarget()) {
            return true;
        }

        // NEW PART: Quick check before expensive search
        if (!HayBaleCache.hasHayBalesNearby(animal.blockPosition(), AutoBreedConfig.HAY_SEARCH_RADIUS.get())) {
            return false;  // No hay bales nearby, don't even bother searching
        }
        
        
        BlockPos newTarget = findHayBlock();
        if (newTarget == null) return false;
        
        targetPos = newTarget;
        return canMoveToTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return isRunning && 
               targetPos != null && 
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
        this.targetPos = null;
        this.pathNav.stop();
    }

    private void updatePath() {
        if (targetPos != null) {
            this.pathNav.moveTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, this.speedModifier);
        }
    }

    @Override
    public void tick() {
        if (targetPos == null || !canMoveToTarget()) {
            stop();
            return;
        }

        // Look at the hay block
        this.animal.getLookControl().setLookAt(
            targetPos.getX() + 0.5,
            targetPos.getY(),
            targetPos.getZ() + 0.5,
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

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public boolean isCloseEnoughToTarget() {
        if (targetPos == null) return false;
        
        // Get the entity's actual position (with decimals)
        Vec3 entityPos = animal.position();
        
        // Calculate distance to the center of the target block
        double dx = entityPos.x - (targetPos.getX() + 0.5);
        double dy = entityPos.y - targetPos.getY();
        double dz = entityPos.z - (targetPos.getZ() + 0.5);
        
        // Use a more generous distance check for larger entities
        double maxDistance = Math.max(1.5D, animal.getBbWidth());
        
        // Check if close enough horizontally and vertically
        return (dx * dx + dz * dz) <= (maxDistance * maxDistance) && 
               Math.abs(dy) <= 1.5D; // Allow more vertical distance for tall entities
    }
}