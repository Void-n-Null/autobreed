package net.voidnull.autobreed.fabric;

import net.fabricmc.api.ModInitializer;
import net.voidnull.autobreed.AutoBreedCommon;

public class FabricAutoBreed implements ModInitializer {
    @Override
    public void onInitialize() {
        // Call the common mod initializer
        AutoBreedCommon.init();

        // Register any Fabric-specific event listeners or configuration,
        // for example using Fabric API event hooks.
    }
} 