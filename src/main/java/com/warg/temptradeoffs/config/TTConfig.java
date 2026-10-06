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

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("triggers");
        NEW_DAY = b.comment("Offer a new choice when a new Minecraft day begins.").define("new_day", true);
        LEVEL_UP = b.comment("Offer a new choice when the player gains a level.").define("level_up", false);
        ON_LOGIN = b.comment("Offer a choice on first login after the world starts, if none has been chosen yet.").define("on_login", false);
        b.pop();
        b.push("selection");
        CHOICES = b.comment("Number of choices shown. Currently supported: 3.").defineInRange("choices", 3, 2, 3);
        DURATION_MINUTES = b.comment("Duration of positive and negative effects.").defineInRange("duration_minutes", 20, 1, 1440);
        COOLDOWN_DAYS = b.comment("Minimum Minecraft days between automatic offers. 0 disables this restriction.").defineInRange("cooldown_days", 0, 0, 1000);
        b.pop();
        SPEC = b.build();
    }
    private TTConfig() {}
}
