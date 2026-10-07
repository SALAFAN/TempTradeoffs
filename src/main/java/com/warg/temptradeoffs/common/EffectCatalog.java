package com.warg.temptradeoffs.common;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class EffectCatalog {
    private static final String MINE_AND_SLASH = "mine_and_slash";

    private static final Map<String, Integer> VANILLA_EFFECT_WEIGHTS = new HashMap<>();

    static {
        // Hidden balance weights. Higher = stronger / more valuable.
        VANILLA_EFFECT_WEIGHTS.put("speed", 1);
        VANILLA_EFFECT_WEIGHTS.put("slowness", 1);
        VANILLA_EFFECT_WEIGHTS.put("haste", 1);
        VANILLA_EFFECT_WEIGHTS.put("mining_fatigue", 1);
        VANILLA_EFFECT_WEIGHTS.put("strength", 2);
        VANILLA_EFFECT_WEIGHTS.put("weakness", 1);
        VANILLA_EFFECT_WEIGHTS.put("jump_boost", 1);
        VANILLA_EFFECT_WEIGHTS.put("nausea", 1);
        VANILLA_EFFECT_WEIGHTS.put("regeneration", 3);
        VANILLA_EFFECT_WEIGHTS.put("resistance", 3);
        VANILLA_EFFECT_WEIGHTS.put("fire_resistance", 2);
        VANILLA_EFFECT_WEIGHTS.put("water_breathing", 1);
        VANILLA_EFFECT_WEIGHTS.put("invisibility", 3);
        VANILLA_EFFECT_WEIGHTS.put("blindness", 2);
        VANILLA_EFFECT_WEIGHTS.put("night_vision", 1);
        VANILLA_EFFECT_WEIGHTS.put("hunger", 1);
        VANILLA_EFFECT_WEIGHTS.put("poison", 2);
        VANILLA_EFFECT_WEIGHTS.put("wither", 4);
        VANILLA_EFFECT_WEIGHTS.put("absorption", 2);
        VANILLA_EFFECT_WEIGHTS.put("saturation", 2);
        VANILLA_EFFECT_WEIGHTS.put("glowing", 1);
        VANILLA_EFFECT_WEIGHTS.put("levitation", 3);
        VANILLA_EFFECT_WEIGHTS.put("slow_falling", 1);
        VANILLA_EFFECT_WEIGHTS.put("conduit_power", 2);
        VANILLA_EFFECT_WEIGHTS.put("dolphins_grace", 1);
        VANILLA_EFFECT_WEIGHTS.put("bad_omen", 1);
        VANILLA_EFFECT_WEIGHTS.put("hero_of_the_village", 3);
        VANILLA_EFFECT_WEIGHTS.put("darkness", 2);
    }

    public static boolean isCraftToExile2Environment() {
        if (!TTConfig.ENABLE_CTE2_INTEGRATION.get()) {
            return false;
        }

        boolean mineAndSlash = ModList.get().isLoaded(MINE_AND_SLASH);
        boolean exileOverlay = ModList.get().isLoaded("exile_overlay");
        boolean libraryOfExile = ModList.get().isLoaded("library_of_exile");

        return mineAndSlash && (exileOverlay || libraryOfExile);
    }

    public static List<Tradeoff.ModifierSpec> positive(Random random) {
        return buildSide(random, true);
    }

    public static List<Tradeoff.ModifierSpec> negative(Random random) {
        return buildSide(random, false);
    }

    private static List<Tradeoff.ModifierSpec> buildSide(Random random, boolean positive) {
        int budget = positive ? TTConfig.POSITIVE_WEIGHT_BUDGET.get() : TTConfig.NEGATIVE_WEIGHT_BUDGET.get();
        int minCount = Math.min(TTConfig.MIN_EFFECTS_PER_SIDE.get(), TTConfig.MAX_EFFECTS_PER_SIDE.get());
        int maxCount = TTConfig.MAX_EFFECTS_PER_SIDE.get();

        List<Tradeoff.ModifierSpec> pool = candidates(positive);
        List<Tradeoff.ModifierSpec> result = new ArrayList<>();
        Set<String> used = new HashSet<>();

        int remaining = budget;

        // First guarantee the configured minimum count whenever possible.
        while (result.size() < minCount) {
            List<Tradeoff.ModifierSpec> fitting = pool.stream()
                    .filter(x -> !used.contains(x.stableKey()))
                    .filter(x -> x.weight() <= remaining)
                    .toList();

            if (fitting.isEmpty()) {
                break;
            }

            Tradeoff.ModifierSpec picked = weightedPick(random, fitting);
            result.add(picked);
            used.add(picked.stableKey());
            remaining -= picked.weight();
        }

        // Fill the remaining hidden budget with a random weighted combination.
        while (result.size() < maxCount && remaining > 0) {
            List<Tradeoff.ModifierSpec> fitting = pool.stream()
                    .filter(x -> !used.contains(x.stableKey()))
                    .filter(x -> x.weight() <= remaining)
                    .toList();

            if (fitting.isEmpty()) {
                break;
            }

            Tradeoff.ModifierSpec picked = weightedPick(random, fitting);
            result.add(picked);
            used.add(picked.stableKey());
            remaining -= picked.weight();

            if (random.nextFloat() < 0.28f && result.size() >= minCount) {
                break;
            }
        }

        // Extremely important: never produce an empty side.
        if (result.isEmpty() && !pool.isEmpty()) {
            result.add(pool.get(random.nextInt(pool.size())));
        }

        return result;
    }

    private static Tradeoff.ModifierSpec weightedPick(Random random, List<Tradeoff.ModifierSpec> pool) {
        // Lower-cost effects are somewhat more common; the hidden weight remains their balance cost.
        double total = 0.0;
        for (Tradeoff.ModifierSpec s : pool) {
            total += 1.0 / Math.max(1, s.weight());
        }

        double roll = random.nextDouble() * total;
        for (Tradeoff.ModifierSpec s : pool) {
            roll -= 1.0 / Math.max(1, s.weight());
            if (roll <= 0) {
                return s;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static List<Tradeoff.ModifierSpec> candidates(boolean positive) {
        List<Tradeoff.ModifierSpec> out = new ArrayList<>();

        if (TTConfig.ENABLE_VANILLA_EFFECTS.get()) {
            for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                MobEffect effect = entry.getValue();

                if (!"minecraft".equals(id.getNamespace())) {
                    continue;
                }

                MobEffectCategory category = effect.getCategory();
                if ((positive && category != MobEffectCategory.BENEFICIAL)
                        || (!positive && category != MobEffectCategory.HARMFUL)) {
                    continue;
                }

                int baseWeight = VANILLA_EFFECT_WEIGHTS.getOrDefault(id.getPath(), 2);
                addEffectVariants(out, id, baseWeight);
            }
        }

        boolean cte2 = isCraftToExile2Environment();

        if (TTConfig.ENABLE_MODDED_EFFECTS.get() || cte2) {
            for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!isAllowedModdedNamespace(id, cte2)) {
                    continue;
                }

                MobEffectCategory category = entry.getValue().getCategory();
                if ((positive && category != MobEffectCategory.BENEFICIAL)
                        || (!positive && category != MobEffectCategory.HARMFUL)) {
                    continue;
                }

                int weight = moddedEffectWeight(id);
                addEffectVariants(out, id, weight);
            }
        }

        if (TTConfig.ENABLE_VANILLA_ATTRIBUTES.get()) {
            for (var entry : BuiltInRegistries.ATTRIBUTE.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!"minecraft".equals(id.getNamespace()) || !isPlayerAttribute(id)) {
                    continue;
                }
                addAttributeVariants(out, id, entry.getValue(), positive);
            }
        }

        if (TTConfig.ENABLE_MODDED_ATTRIBUTES.get() || cte2) {
            for (var entry : BuiltInRegistries.ATTRIBUTE.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!isAllowedModdedNamespace(id, cte2) || !isPlayerAttribute(id)) {
                    continue;
                }
                addAttributeVariants(out, id, entry.getValue(), positive);
            }
        }

        return out;
    }

    private static boolean isAllowedModdedNamespace(ResourceLocation id, boolean cte2) {
        if ("minecraft".equals(id.getNamespace())) {
            return false;
        }
        if (cte2 && MINE_AND_SLASH.equals(id.getNamespace())) {
            return true;
        }
        return TTConfig.ENABLE_MODDED_EFFECTS.get() || TTConfig.ENABLE_MODDED_ATTRIBUTES.get();
    }

    private static void addEffectVariants(List<Tradeoff.ModifierSpec> out, ResourceLocation id, int baseWeight) {
        int[] weights = {Math.max(1, baseWeight), Math.max(1, baseWeight + 1)};
        int[] amplifiers = {0, 1};

        for (int i = 0; i < weights.length; i++) {
            out.add(new Tradeoff.ModifierSpec(
                    Tradeoff.ModifierType.MOB_EFFECT,
                    id,
                    amplifiers[i],
                    0.0,
                    AttributeModifier.Operation.ADDITION,
                    weights[i]
            ));
        }
    }

    private static void addAttributeVariants(
            List<Tradeoff.ModifierSpec> out,
            ResourceLocation id,
            Attribute attribute,
            boolean positive
    ) {
        // Three hidden power tiers. The actual value is deliberately not shown as a "weight".
        double[] amounts = {0.05, 0.10, 0.20};
        int[] weights = {1, 2, 4};

        for (int i = 0; i < amounts.length; i++) {
            double amount = positive ? amounts[i] : -amounts[i];
            out.add(new Tradeoff.ModifierSpec(
                    Tradeoff.ModifierType.ATTRIBUTE,
                    id,
                    0,
                    amount,
                    AttributeModifier.Operation.MULTIPLY_BASE,
                    weights[i]
            ));
        }
    }

    private static boolean isPlayerAttribute(ResourceLocation id) {
        String p = id.getPath();

        // Vanilla attributes useful for a player and common Mine and Slash RPG attributes.
        if ("minecraft".equals(id.getNamespace())) {
            return p.equals("generic.max_health")
                    || p.equals("generic.follow_range")
                    || p.equals("generic.knockback_resistance")
                    || p.equals("generic.movement_speed")
                    || p.equals("generic.flying_speed")
                    || p.equals("generic.attack_damage")
                    || p.equals("generic.attack_knockback")
                    || p.equals("generic.attack_speed")
                    || p.equals("generic.armor")
                    || p.equals("generic.armor_toughness")
                    || p.equals("generic.luck")
                    || p.equals("generic.reach_distance");
        }

        return MINE_AND_SLASH.equals(id.getNamespace());
    }

    private static int moddedEffectWeight(ResourceLocation id) {
        String p = id.getPath();

        // Known Mine and Slash-style status effects get more conservative balance costs.
        if (p.contains("wither") || p.contains("wounds") || p.contains("bleed")) return 4;
        if (p.contains("blind") || p.contains("weakness") || p.contains("slow")) return 2;
        if (p.contains("regen") || p.contains("barrier") || p.contains("fortify")) return 3;
        if (p.contains("burn") || p.contains("poison") || p.contains("venom")) return 3;
        return 2;
    }

    private EffectCatalog() {}
}
