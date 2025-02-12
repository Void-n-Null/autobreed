package net.voidnull.autobreed;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AutoBreedConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue FOOD_EATING_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue BABY_GROWTH_BOOST_TICKS;
    public static final ModConfigSpec.IntValue SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue SEARCH_VERTICAL_RADIUS;

    static {
        BUILDER.comment("AutoBreed Configuration");
        BUILDER.push("general");

        FOOD_EATING_COOLDOWN_TICKS = BUILDER
            .comment("How many ticks an animal must wait between eating food items (20 ticks = 1 second)",
                    "Default: 20 ticks (1 second)")
            .defineInRange("foodEatingCooldownTicks", 20, 1, 1200);

        BABY_GROWTH_BOOST_TICKS = BUILDER
            .comment("How many ticks of growth a baby animal gets from eating food",
                    "Default: 200 ticks (10 seconds of growth)")
            .defineInRange("babyGrowthBoostTicks", 200, 1, 6000);

        SEARCH_RADIUS = BUILDER
            .comment("How far (in blocks) animals will search horizontally for food sources (items, hay bales, item frames)",
                    "Default: 8 blocks")
            .defineInRange("searchRadius", 8, 1, 16);

        SEARCH_VERTICAL_RADIUS = BUILDER
            .comment("How far (in blocks) animals will search vertically for food sources",
                    "Default: 4 blocks")
            .defineInRange("searchVerticalRadius", 4, 1, 8);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 