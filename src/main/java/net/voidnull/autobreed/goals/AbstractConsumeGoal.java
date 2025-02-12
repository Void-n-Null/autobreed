package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.EnumSet;

public abstract class AbstractConsumeGoal<T, G extends AbstractTargetGoal<T>> extends Goal {
    protected final Animal animal;
    protected final G targetGoal;
    protected static final int BABY_GROWTH_TICKS = 200; // 10 seconds worth of growth
    protected int cooldown = 0;
    protected T targetResource = null;

    protected AbstractConsumeGoal(Animal animal, G targetGoal) {
        this.animal = animal;
        this.targetGoal = targetGoal;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    protected abstract int getEatingCooldownTicks();
    protected abstract void consumeTarget();
    protected abstract boolean isTargetValid(T target);
    protected abstract boolean canConsumeTarget(T target);

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        if (!targetGoal.isCloseEnoughToTarget()) {
            return false;
        }
        
        T target = targetGoal.getTarget();
        if (target == null || !isTargetValid(target)) {
            return false;
        }

        if (animal.isBaby()) {
            targetResource = target;
            return true;
        }

        if (animal.isInLove()) return false;
        if (!animal.canFallInLove()) return false;
        if (animal.canBreed()) return false;
        if (animal.getAge() != 0) return false;

        if (!canConsumeTarget(target)) return false;

        targetResource = target;
        return true;
    }

    @Override
    public void start() {
        if (targetResource == null) {
            return;
        }

        if (animal.level().isClientSide()) {
            return;
        }

        // Play eating sound
        animal.level().playSound(null, animal, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);

        // Consume the target
        consumeTarget();

        // Set cooldown
        cooldown = getEatingCooldownTicks();

        // Handle growth or breeding
        if (animal.isBaby()) {
            animal.ageUp(BABY_GROWTH_TICKS);
        } else {
            animal.setInLove(null);
        }

        // Clear the target
        targetResource = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // This is a one-shot action
    }
} 