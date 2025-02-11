package net.voidnull.autobreed.forge;

import net.minecraftforge.fml.common.Mod;
import net.voidnull.autobreed.AutoBreedCommon;

@Mod(AutoBreedCommon.MODID)
public class ForgeAutoBreed {
    public ForgeAutoBreed() {
        AutoBreedCommon.getInstance().initialize();
        // Forge-specific initialization
    }
} 