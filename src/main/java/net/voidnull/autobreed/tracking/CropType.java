package net.voidnull.autobreed.tracking;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public enum CropType {
    WHEAT(Blocks.WHEAT, Items.WHEAT),
    CARROTS(Blocks.CARROTS, Items.CARROT),
    POTATOES(Blocks.POTATOES, Items.POTATO);

    private final Block cropBlock;
    private final Item cropItem;

    CropType(Block cropBlock, Item cropItem) {
        this.cropBlock = cropBlock;
        this.cropItem = cropItem;
    }

    public Block getCropBlock() {
        return cropBlock;
    }

    public Item getCropItem() {
        return cropItem;
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