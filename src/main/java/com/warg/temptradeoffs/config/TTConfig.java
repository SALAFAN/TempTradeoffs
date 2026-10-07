package com.warg.temptradeoffs.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TTConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.IntValue HUD_X;
    public static final ForgeConfigSpec.IntValue HUD_Y;

    public static final ForgeConfigSpec.BooleanValue NEW_DAY;
    public static final ForgeConfigSpec.BooleanValue LEVEL_UP;
    public static final ForgeConfigSpec.BooleanValue ON_LOGIN;

    public static final ForgeConfigSpec.IntValue CHOICES;
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
    public static final ForgeConfigSpec.BooleanValue FREEZE_WHILE_CHOOSING;
    public static final ForgeConfigSpec.BooleanValue HUD_BUTTON;
    public static final ForgeConfigSpec.IntValue MAX_SELECTION_WEIGHT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POSITIVE_EFFECT_WEIGHTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NEGATIVE_EFFECT_WEIGHTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POSITIVE_EFFECT_VALUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NEGATIVE_EFFECT_VALUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> POSITIVE_EFFECT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NEGATIVE_EFFECT_ENABLED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("triggers");
        NEW_DAY = b.comment("Offer a new choice when a new Minecraft day begins.")
                .define("new_day", true);
        LEVEL_UP = b.comment("Offer a new choice when the player's XP level changes.")
                .define("level_up", true);
        ON_LOGIN = b.comment("Offer a choice on first login if the player has never chosen one.")
                .define("on_login", false);
        b.pop();

        b.push("selection");
        CHOICES = b.comment("Number of cards shown.")
                .defineInRange("choices", 3, 2, 3);
        COOLDOWN_DAYS = b.comment("Minimum Minecraft days between automatic offers. 0 disables this restriction.")
                .defineInRange("cooldown_days", 0, 0, 1000);
        POSITIVE_WEIGHT_BUDGET = b.comment("Hidden total weight budget for positive modifiers in each card.")
                .defineInRange("positive_weight_budget", 1000, 1, 10000);
        NEGATIVE_WEIGHT_BUDGET = b.comment("Hidden total weight budget for negative modifiers in each card.")
                .defineInRange("negative_weight_budget", 1000, 1, 10000);
        MAX_SELECTION_WEIGHT = b.comment("Hard upper limit for the total weight budget used by either side of a generated card.")
                .defineInRange("max_selection_weight", 1000, 1, 10000);
        POSITIVE_EFFECT_WEIGHTS = b.comment("Per-modifier positive weight overrides. Managed by the in-game config screen.")
                .defineListAllowEmpty("positive_effect_weights", List.of(), o -> o instanceof String && ((String)o).contains("="));
        NEGATIVE_EFFECT_WEIGHTS = b.comment("Per-modifier negative weight overrides. Managed by the in-game config screen.")
                .defineListAllowEmpty("negative_effect_weights", List.of(), o -> o instanceof String && ((String)o).contains("="));
        POSITIVE_EFFECT_VALUES = b.comment("Per-modifier positive value overrides. Format key=value.")
                .defineListAllowEmpty("positive_effect_values", List.of(), o -> o instanceof String && ((String)o).contains("="));
        NEGATIVE_EFFECT_VALUES = b.comment("Per-modifier negative value overrides. Format key=value.")
                .defineListAllowEmpty("negative_effect_values", List.of(), o -> o instanceof String && ((String)o).contains("="));
        POSITIVE_EFFECT_ENABLED = b.comment("Disabled positive modifier families. Format type:id=true/false.")
                .defineListAllowEmpty("positive_effect_enabled", List.of(), o -> o instanceof String && ((String)o).contains("="));
        NEGATIVE_EFFECT_ENABLED = b.comment("Disabled negative modifier families. Format type:id=true/false.")
                .defineListAllowEmpty("negative_effect_enabled", List.of(), o -> o instanceof String && ((String)o).contains("="));
        MIN_EFFECTS_PER_SIDE = b.comment("Minimum positive/negative modifiers in one card.")
                .defineInRange("min_effects_per_side", 1, 1, 10);
        MAX_EFFECTS_PER_SIDE = b.comment("Maximum positive/negative modifiers in one card.")
                .defineInRange("max_effects_per_side", 10, 1, 10);
        b.pop();

        b.push("pool");
        ENABLE_VANILLA_EFFECTS = b.comment("Use vanilla MobEffects.")
                .define("vanilla_effects", true);
        ENABLE_VANILLA_ATTRIBUTES = b.comment("Use vanilla player attributes.")
                .define("vanilla_attributes", true);
        ENABLE_MODDED_EFFECTS = b.comment("Use compatible modded MobEffects.")
                .define("modded_effects", true);
        ENABLE_MODDED_ATTRIBUTES = b.comment("Use compatible modded Attributes.")
                .define("modded_attributes", true);
        ENABLE_CTE2_INTEGRATION = b.comment("Enable the extended Craft to Exile 2 / Mine and Slash pool when detected.")
                .define("craft_to_exile_2_integration", true);
        b.pop();

        b.push("interface");
        FREEZE_WHILE_CHOOSING = b.comment("Freeze the player while the mandatory choice screen is open.")
                .define("freeze_while_choosing", true);
        HUD_BUTTON = b.comment("Show the current TempTradeoffs choice button when the player inventory is open.")
                .define("hud_button", true);
        b.pop();

        SPEC = b.build();

        ForgeConfigSpec.Builder cb = new ForgeConfigSpec.Builder();
        cb.push("hud");
        HUD_X = cb.comment("Saved X position of the fish button. -1 uses the automatic position next to FTB buttons.")
                .defineInRange("x", -1, -10000, 10000);
        HUD_Y = cb.comment("Saved Y position of the fish button. -1 uses the automatic position next to FTB buttons.")
                .defineInRange("y", -1, -10000, 10000);
        cb.pop();
        CLIENT_SPEC = cb.build();
    }


    public static int getWeightOverride(String key, int fallback, boolean positive) {
        List<? extends String> values = positive ? POSITIVE_EFFECT_WEIGHTS.get() : NEGATIVE_EFFECT_WEIGHTS.get();
        for (String entry : values) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0) continue;
            if (!entry.substring(0, eq).equals(key)) continue;
            try { return Math.max(1, Integer.parseInt(entry.substring(eq + 1).trim())); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    public static void setWeightOverride(String key, int weight, boolean positive) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> cfg = positive ? POSITIVE_EFFECT_WEIGHTS : NEGATIVE_EFFECT_WEIGHTS;
        List<String> out = new ArrayList<>();
        for (String entry : cfg.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq > 0 && entry.substring(0, eq).equals(key)) continue;
            out.add(entry);
        }
        out.add(key + "=" + Math.max(1, weight));
        cfg.set(out);
        SPEC.save();
        com.warg.temptradeoffs.common.EffectCatalog.invalidateCache();
    }


    public static double getValueOverride(String key, double fallback, boolean positive) {
        List<? extends String> values = positive ? POSITIVE_EFFECT_VALUES.get() : NEGATIVE_EFFECT_VALUES.get();
        for (String entry : values) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0 || !entry.substring(0, eq).equals(key)) continue;
            try { return Double.parseDouble(entry.substring(eq + 1).trim()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        return fallback;
    }

    public static void setValueOverride(String key, double value, boolean positive) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> cfg = positive ? POSITIVE_EFFECT_VALUES : NEGATIVE_EFFECT_VALUES;
        List<String> out = new ArrayList<>();
        for (String entry : cfg.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq > 0 && entry.substring(0, eq).equals(key)) continue;
            out.add(entry);
        }
        out.add(key + "=" + String.format(Locale.ROOT, "%.6f", value));
        cfg.set(out);
        SPEC.save();
        com.warg.temptradeoffs.common.EffectCatalog.invalidateCache();
    }

    public static boolean isModifierEnabled(String key, boolean positive) {
        List<? extends String> values = positive ? POSITIVE_EFFECT_ENABLED.get() : NEGATIVE_EFFECT_ENABLED.get();
        for (String entry : values) {
            int eq = entry.lastIndexOf('=');
            if (eq <= 0 || !entry.substring(0, eq).equals(key)) continue;
            return Boolean.parseBoolean(entry.substring(eq + 1).trim());
        }
        return true;
    }

    public static void setModifierEnabled(String key, boolean enabled, boolean positive) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> cfg = positive ? POSITIVE_EFFECT_ENABLED : NEGATIVE_EFFECT_ENABLED;
        List<String> out = new ArrayList<>();
        for (String entry : cfg.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq > 0 && entry.substring(0, eq).equals(key)) continue;
            out.add(entry);
        }
        out.add(key + "=" + enabled);
        cfg.set(out);
        SPEC.save();
        com.warg.temptradeoffs.common.EffectCatalog.invalidateCache();
    }

    public static void removeWeightOverride(String key, boolean positive) {
        ForgeConfigSpec.ConfigValue<List<? extends String>> cfg = positive ? POSITIVE_EFFECT_WEIGHTS : NEGATIVE_EFFECT_WEIGHTS;
        List<String> out = new ArrayList<>();
        for (String entry : cfg.get()) {
            int eq = entry.lastIndexOf('=');
            if (eq > 0 && entry.substring(0, eq).equals(key)) continue;
            out.add(entry);
        }
        cfg.set(out);
        SPEC.save();
    }

    private TTConfig() {}
}
