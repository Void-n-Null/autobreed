package net.voidnull.autobreed;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.voidnull.autobreed.goals.TargetFoodGoal;
import net.voidnull.autobreed.goals.ConsumeFoodGoal;
import net.voidnull.autobreed.goals.TargetHayBlockGoal;
import net.voidnull.autobreed.goals.ConsumeHayBaleGoal;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AutoBreed.MOD_ID)
public class AutoBreed {
    public static final String MOD_ID = "autobreed";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AutoBreed(IEventBus modEventBus) {
        LOGGER.info("Initializing AutoBreed Mod");
        
        // Register ourselves for mod events
        modEventBus.addListener(this::commonSetup);
        
        // Register for forge events
        NeoForge.EVENT_BUS.register(AutobreedEventHandler.class);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
    
    @SubscribeEvent
    public void onAnimalJoinWorld(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Animal animal) {
            // Food item goals (for all animals)
            TargetFoodGoal targetFoodGoal = new TargetFoodGoal(animal);
            ConsumeFoodGoal consumeFoodGoal = new ConsumeFoodGoal(animal, targetFoodGoal);
            
            // Add food goals
            animal.goalSelector.addGoal(1, consumeFoodGoal);   // Highest priority for food consumption
            animal.goalSelector.addGoal(2, targetFoodGoal);    // High priority for food movement
            
            // Only add hay bale goals for animals that eat wheat
            if (animal.isFood(Items.WHEAT.getDefaultInstance())) {
                // Hay bale goals
                TargetHayBlockGoal targetHayGoal = new TargetHayBlockGoal(animal);
                ConsumeHayBaleGoal consumeHayGoal = new ConsumeHayBaleGoal(animal, targetHayGoal);
                
                // Add hay goals
                animal.goalSelector.addGoal(1, consumeHayGoal);    // Equal priority with food consumption
                animal.goalSelector.addGoal(2, targetHayGoal);     // Equal priority with food movement
            }
        }
    }
} 