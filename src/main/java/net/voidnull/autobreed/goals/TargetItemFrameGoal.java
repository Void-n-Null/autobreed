package net.voidnull.autobreed.goals;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class TargetItemFrameGoal extends AbstractEntityTargetGoal<ItemFrame> {

    public TargetItemFrameGoal(Animal animal) {
        this(animal, 1.0D);
    }

    public TargetItemFrameGoal(Animal animal, double speedModifier) {
        super(animal, speedModifier);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    protected Class<ItemFrame> getTargetClass() {
        return ItemFrame.class;
    }

    @Override
    protected boolean isValidFoodSource(ItemFrame entity) {
        return animal.isFood(entity.getItem());
    }

    @Override
    protected boolean isValidTarget(ItemFrame target) {
        return isValidFoodSource(target);
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