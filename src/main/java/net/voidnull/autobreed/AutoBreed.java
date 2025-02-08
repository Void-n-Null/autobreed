package net.voidnull.autobreed;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.animal.Animal;
import net.voidnull.autobreed.goals.EatDroppedFoodGoal;
import net.voidnull.autobreed.goals.TargetFoodGoal;
import net.voidnull.autobreed.goals.ConsumeFoodGoal;
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
    public void onAnimalJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Animal animal) {
            LOGGER.info("Adding food goals to animal: {}", animal);
            
            TargetFoodGoal targetGoal = new TargetFoodGoal(animal);
            ConsumeFoodGoal consumeGoal = new ConsumeFoodGoal(animal, targetGoal);
            
            // Make food goals high priority
            animal.goalSelector.addGoal(2, targetGoal);   // Very high priority for movement
            animal.goalSelector.addGoal(1, consumeGoal);  // Highest priority for consumption
            
            LOGGER.info("Added food goals to animal: {} with priorities 1 and 2", animal);
        }
    }
} 