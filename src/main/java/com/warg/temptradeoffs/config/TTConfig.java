package com.warg.temptradeoffs.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TTConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue NEW_DAY;
    public static final ForgeConfigSpec.BooleanValue LEVEL_UP;
    public static final ForgeConfigSpec.BooleanValue ON_LOGIN;

    public static final ForgeConfigSpec.IntValue CHOICES;
    public static final ForgeConfigSpec.IntValue DURATION_MINUTES;
    public static final ForgeConfigSpec.IntValue COOLDOWN_DAYS;

    public static final ForgeConfigSpec.IntValue POSITIVE_WEIGHT_BUDGET;
    public static final ForgeConfigSpec.IntValue NEGATIVE_WEIGHT_BUDGET;
    public static final ForgeConfigSpec.IntValue MIN_EFFECTS_PER_SIDE;
    public static final ForgeConfigSpec.IntValue MAX_EFFECTS_PER_SIDE;

    public static final ForgeConfigSpec.BooleanValue ENABLE_VANILLA_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VANILLA_ATTRIBUTES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MODDED_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MODDED_ATTRIBUTES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CTE2_INTEGRATION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("triggers");
        NEW_DAY = b.comment("Offer a new choice when a new Minecraft day begins.")
                .define("new_day", true);
        LEVEL_UP = b.comment("Offer a new choice when the player gains a level.")
                .define("level_up", false);
        ON_LOGIN = b.comment("Offer a choice on first login if the player has never chosen one.")
                .define("on_login", false);
        b.pop();

        b.push("selection");
        CHOICES = b.comment("Number of cards shown.")
                .defineInRange("choices", 3, 2, 3);
        DURATION_MINUTES = b.comment("Duration of all selected modifiers.")
                .defineInRange("duration_minutes", 20, 1, 1440);
        COOLDOWN_DAYS = b.comment("Minimum Minecraft days between automatic offers. 0 disables this restriction.")
                .defineInRange("cooldown_days", 0, 0, 1000);
        POSITIVE_WEIGHT_BUDGET = b.comment("Hidden total weight budget for positive modifiers in each card.")
                .defineInRange("positive_weight_budget", 6, 1, 50);
        NEGATIVE_WEIGHT_BUDGET = b.comment("Hidden total weight budget for negative modifiers in each card.")
                .defineInRange("negative_weight_budget", 6, 1, 50);
        MIN_EFFECTS_PER_SIDE = b.comment("Minimum number of positive/negative modifiers in one card.")
                .defineInRange("min_effects_per_side", 1, 1, 8);
        MAX_EFFECTS_PER_SIDE = b.comment("Maximum number of positive/negative modifiers in one card.")
                .defineInRange("max_effects_per_side", 4, 1, 8);
        b.pop();

        b.push("pool");
        ENABLE_VANILLA_EFFECTS = b.comment("Use vanilla potion/status effects.")
                .define("vanilla_effects", true);
        ENABLE_VANILLA_ATTRIBUTES = b.comment("Use vanilla player attributes.")
                .define("vanilla_attributes", true);
        ENABLE_MODDED_EFFECTS = b.comment("Use compatible modded MobEffects. Craft to Exile 2 mode enables its own extended pool.")
                .define("modded_effects", true);
        ENABLE_MODDED_ATTRIBUTES = b.comment("Use compatible modded attributes. Craft to Exile 2 mode enables its own extended pool.")
                .define("modded_attributes", true);
        ENABLE_CTE2_INTEGRATION = b.comment("Automatically enable the extended Craft to Exile 2 / Mine and Slash pool when its characteristic mods are detected.")
                .define("craft_to_exile_2_integration", true);
        b.pop();

        SPEC = b.build();
    }

    private TTConfig() {}
}
