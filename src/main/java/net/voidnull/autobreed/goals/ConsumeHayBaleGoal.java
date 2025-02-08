package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.voidnull.autobreed.HayBaleDataManager; // Import the data manager
import java.util.EnumSet;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class ConsumeHayBaleGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Animal animal;
    private final TargetHayBlockGoal targetGoal; // Assuming you have this
    private static final int EATING_COOLDOWN_TICKS = 40;
    private static final int BABY_GROWTH_TICKS = 200;
    private int cooldown = 0;
    private BlockPos targetHayBale = null;

    public ConsumeHayBaleGoal(Animal animal, TargetHayBlockGoal targetGoal) {
        this.animal = animal;
        this.targetGoal = targetGoal;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

       @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            LOGGER.debug("{} on cooldown: {}", animal.getName().getString(), cooldown);
            return false;
        }

        if (!targetGoal.isCloseEnoughToTarget()) {
            LOGGER.debug("{} not close enough to target", animal.getName().getString());
            return false;
        }
        
        BlockPos hayPos = targetGoal.getTargetPos();
        if (hayPos == null) {
            LOGGER.debug("{} has no target hay position", animal.getName().getString());
            return false;
        }
        
        BlockState state = animal.level().getBlockState(hayPos);
        if (!state.is(Blocks.HAY_BLOCK)) {
            LOGGER.debug("{} target is not a hay block at {}", animal.getName().getString(), hayPos);
            return false;
        }

        // Check if we're actually on top of or adjacent to the hay block
        BlockPos animalPos = animal.blockPosition();
        if (!isNextToOrAbove(animalPos, hayPos)) {
            LOGGER.debug("{} not next to or above hay block. Animal at {}, hay at {}", 
                animal.getName().getString(), animalPos, hayPos);
            return false;
        }

        targetHayBale = hayPos;
        LOGGER.debug("Target hay bale set to {} for {}", hayPos, animal.getName().getString());

        // Different conditions for babies and adults
        if (animal.isBaby()) {
            LOGGER.debug("{} is baby, can eat hay", animal.getName().getString());
            return true; // Babies can always eat to grow
        } else {
            boolean canEat = !animal.isInLove() && 
                           animal.canFallInLove() && 
                           !animal.canBreed() && 
                           animal.getAge() == 0 &&
                           animal.isFood(Items.WHEAT.getDefaultInstance());
            
            LOGGER.debug("{} adult conditions: inLove={}, canFallInLove={}, canBreed={}, age={}, eatsWheat={}", 
                animal.getName().getString(),
                animal.isInLove(),
                animal.canFallInLove(),
                animal.canBreed(),
                animal.getAge(),
                animal.isFood(Items.WHEAT.getDefaultInstance()));
            
            return canEat;
        }
    }

    private boolean isNextToOrAbove(BlockPos animalPos, BlockPos hayPos) {
        if (animalPos.equals(hayPos.above())) {
            LOGGER.debug("{} is above hay block", animal.getName().getString());
            return true; // On top
        }
        
        // Check all adjacent positions
        boolean isAdjacent = animalPos.equals(hayPos.north()) ||
                           animalPos.equals(hayPos.south()) ||
                           animalPos.equals(hayPos.east()) ||
                           animalPos.equals(hayPos.west());
        
        if (isAdjacent) {
            LOGGER.debug("{} is adjacent to hay block", animal.getName().getString());
        }
        
        return isAdjacent;
    }

    @Override
    public void start() {
        if (targetHayBale == null) {
            return;
        }

        // No client-side checks needed!  We only care about the server.

        // Server-side logic
        BlockState blockState = animal.level().getBlockState(targetHayBale);
		if (!blockState.is(Blocks.HAY_BLOCK)) {
			return;
		}

        // Use the Data Manager! No more capability checks.
        HayBaleDataManager.decrementEatenCount(targetHayBale, animal.level());

        // Play eating sound (moved here)
        animal.level().playSound(null, animal, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        cooldown = EATING_COOLDOWN_TICKS;

        if (animal.isBaby()) {
            animal.ageUp(BABY_GROWTH_TICKS);
        } else {
            animal.setInLove(null);
        }

        targetHayBale = null; // Clear target
    }

    @Override
    public boolean canContinueToUse() {
        return false; // This is a one-shot action
    }
}