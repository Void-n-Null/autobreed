package net.voidnull.autobreed.crops;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public enum CropType {
    WHEAT(Blocks.WHEAT, Items.WHEAT, 7),
    CARROTS(Blocks.CARROTS, Items.CARROT, 7),
    POTATOES(Blocks.POTATOES, Items.POTATO, 7);

    private final Block cropBlock;
    private final Item cropItem;
    private final int maxAge;

    CropType(Block cropBlock, Item cropItem, int maxAge) {
        this.cropBlock = cropBlock;
        this.cropItem = cropItem;
        this.maxAge = maxAge;
    }

    public Block getCropBlock() {
        return cropBlock;
    }

    public Item getCropItem() {
        return cropItem;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public boolean isFullyGrown(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        return false;
    }

    public boolean matches(BlockState state) {
        return state.is(cropBlock);
    }
} 