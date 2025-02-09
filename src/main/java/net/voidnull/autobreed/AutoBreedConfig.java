package net.voidnull.autobreed;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AutoBreedConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue HAY_EATING_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue BABY_GROWTH_BOOST_TICKS;
    public static final ModConfigSpec.IntValue HAY_SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue HAY_SEARCH_VERTICAL_RADIUS;

    static {
        BUILDER.comment("AutoBreed Configuration");
        BUILDER.push("general");

        HAY_EATING_COOLDOWN_TICKS = BUILDER
            .comment("How many ticks an animal must wait between eating hay (20 ticks = 1 second)",
                    "Default: 40 ticks (2 seconds)")
            .defineInRange("hayEatingCooldownTicks", 40, 1, 1200);

        BABY_GROWTH_BOOST_TICKS = BUILDER
            .comment("How many ticks of growth a baby animal gets from eating hay",
                    "Default: 200 ticks (10 seconds of growth)")
            .defineInRange("babyGrowthBoostTicks", 200, 1, 6000);

        HAY_SEARCH_RADIUS = BUILDER
            .comment("How far (in blocks) animals will search horizontally for hay blocks",
                    "Default: 8 blocks")
            .defineInRange("haySearchRadius", 8, 1, 16);

        HAY_SEARCH_VERTICAL_RADIUS = BUILDER
            .comment("How far (in blocks) animals will search vertically for hay blocks",
                    "Default: 4 blocks")
            .defineInRange("haySearchVerticalRadius", 4, 1, 8);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
} 