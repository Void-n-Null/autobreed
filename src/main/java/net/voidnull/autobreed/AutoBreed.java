package net.voidnull.autobreed;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.animal.Animal;
import net.voidnull.autobreed.goals.EatDroppedFoodGoal;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(AutoBreed.MOD_ID)
public class AutoBreed {
    public static final String MOD_ID = "autobreed";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AutoBreed(IEventBus modEventBus) {
        LOGGER.info("Initializing AutoBreed Mod");
        
        // Register ourselves for mod events
        modEventBus.addListener(this::commonSetup);
        
        // Register for forge events
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AutoBreed Common Setup");
    }
    
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Animal animal) {
            animal.goalSelector.addGoal(3, new EatDroppedFoodGoal(animal));
            LOGGER.debug("Added EatDroppedFoodGoal to {}", animal);
        }
    }
} 