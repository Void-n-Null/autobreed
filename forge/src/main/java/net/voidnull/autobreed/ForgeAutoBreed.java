package net.voidnull.autobreed;

import net.minecraftforge.fml.common.Mod;

@Mod(AutoBreedCommon.MODID)
public class ForgeAutoBreed {
    // Forge constructs the mod instance via the class constructor.
    public ForgeAutoBreed() {
        // Call the common mod initializer.
        AutoBreedCommon.init();

        // Register Forge-specific event listeners here using Forge event bus APIs.
    }
} 