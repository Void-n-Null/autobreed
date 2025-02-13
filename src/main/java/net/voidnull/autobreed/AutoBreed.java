package net.voidnull.autobreed;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Items;
import net.voidnull.autobreed.goals.*;
import net.voidnull.autobreed.tracking.*;
import net.voidnull.autobreed.crops.CropType;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AutoBreed.MODID)
public class AutoBreed {
    public static final String MODID = "autobreed";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // The main block tracking handler
    private static BlockTrackingHandler blockTracker;
    
    public AutoBreed(IEventBus modEventBus) {
        LOGGER.info("AutoBreed mod initialization starting...");
        
        // Get the mod container from the ModLoadingContext
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        
        // Register our configuration
        container.registerConfig(ModConfig.Type.COMMON, AutoBreedConfig.SPEC);
        
        // Register for mod events
        modEventBus.addListener(this::commonSetup);
        
        // Initialize our block tracking system
        blockTracker = new BlockTrackingHandler();
        
        // Register for forge events
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(blockTracker);
        
        LOGGER.info("AutoBreed mod initialization completed.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Any common setup code goes here
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
            
            //Separate goal for item frames.
            //Leave it at a lower priority than food goals.
            //When the animal has nothing better to do, it will go after item frames.
            animal.goalSelector.addGoal(3, new TargetItemFrameGoal(animal));
            
            // Only add hay bale and crop goals for animals that eat wheat
            if (animal.isFood(Items.WHEAT.getDefaultInstance())) {
                // Hay bale goals
                TargetHayBlockGoal targetHayGoal = new TargetHayBlockGoal(animal, blockTracker.getHayBaleTracker());
                ConsumeHayBaleGoal consumeHayGoal = new ConsumeHayBaleGoal(animal, targetHayGoal);
                
                // Add hay bale goals with slightly lower priority
                animal.goalSelector.addGoal(3, consumeHayGoal);
                animal.goalSelector.addGoal(4, targetHayGoal);
                
                // Add crop goals for wheat
                addCropGoals(animal, CropType.WHEAT, 1);
            }
            
            // Add carrot goals for animals that eat carrots
            if (animal.isFood(Items.CARROT.getDefaultInstance())) {
                addCropGoals(animal, CropType.CARROTS, 1);
            }
            
            // Add potato goals for animals that eat potatoes
            if (animal.isFood(Items.POTATO.getDefaultInstance())) {
                addCropGoals(animal, CropType.POTATOES, 1);
            }
        }
    }
    
    private void addCropGoals(Animal animal, CropType cropType, int priority) {
        TrackedCrop tracker = blockTracker.getCropTracker(cropType);
        TargetCropGoal targetCropGoal = new TargetCropGoal(animal, tracker);
        ConsumeCropGoal consumeCropGoal = new ConsumeCropGoal(animal, targetCropGoal);
        
        animal.goalSelector.addGoal(priority, consumeCropGoal);
        animal.goalSelector.addGoal(priority + 1, targetCropGoal);
    }
    
    // Provide access to the block tracker
    public static BlockTrackingHandler getBlockTracker() {
        return blockTracker;
    }
} 