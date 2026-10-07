package com.warg.temptradeoffs.common;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;

import java.util.*;

public final class EffectCatalog {
    private static final String MINE_AND_SLASH = "mmorpg";
    private static final Map<String, Integer> VANILLA_WEIGHTS = new HashMap<>();

    static {
        VANILLA_WEIGHTS.put("speed", 25); VANILLA_WEIGHTS.put("slowness", 25);
        VANILLA_WEIGHTS.put("haste", 25); VANILLA_WEIGHTS.put("mining_fatigue", 25);
        VANILLA_WEIGHTS.put("strength", 60); VANILLA_WEIGHTS.put("weakness", 30);
        VANILLA_WEIGHTS.put("jump_boost", 20); VANILLA_WEIGHTS.put("nausea", 20);
        VANILLA_WEIGHTS.put("regeneration", 90); VANILLA_WEIGHTS.put("resistance", 90);
        VANILLA_WEIGHTS.put("fire_resistance", 55); VANILLA_WEIGHTS.put("water_breathing", 25);
        VANILLA_WEIGHTS.put("invisibility", 80); VANILLA_WEIGHTS.put("blindness", 45);
        VANILLA_WEIGHTS.put("night_vision", 25); VANILLA_WEIGHTS.put("hunger", 25);
        VANILLA_WEIGHTS.put("poison", 55); VANILLA_WEIGHTS.put("wither", 120);
        VANILLA_WEIGHTS.put("absorption", 50); VANILLA_WEIGHTS.put("saturation", 55);
        VANILLA_WEIGHTS.put("glowing", 25); VANILLA_WEIGHTS.put("levitation", 85);
        VANILLA_WEIGHTS.put("slow_falling", 25); VANILLA_WEIGHTS.put("conduit_power", 55);
        VANILLA_WEIGHTS.put("dolphins_grace", 25); VANILLA_WEIGHTS.put("bad_omen", 35);
        VANILLA_WEIGHTS.put("hero_of_the_village", 90); VANILLA_WEIGHTS.put("darkness", 50);
    }

    public static boolean isCraftToExile2Environment() {
        if (!TTConfig.ENABLE_CTE2_INTEGRATION.get()) return false;
        // Mine and Slash is registered as "mmorpg" in the CTE2 environment.
        return ModList.get().isLoaded(MINE_AND_SLASH)
                && (ModList.get().isLoaded("exile_overlay") || ModList.get().isLoaded("library_of_exile"));
    }

    public static List<Tradeoff.ModifierSpec> positive(Random random) { return buildSide(random, true); }
    public static List<Tradeoff.ModifierSpec> negative(Random random) { return buildSide(random, false); }

    private static List<Tradeoff.ModifierSpec> buildSide(Random random, boolean positive) {
        int budget = positive ? TTConfig.POSITIVE_WEIGHT_BUDGET.get() : TTConfig.NEGATIVE_WEIGHT_BUDGET.get();
        int minCount = Math.min(TTConfig.MIN_EFFECTS_PER_SIDE.get(), TTConfig.MAX_EFFECTS_PER_SIDE.get());
        int maxCount = TTConfig.MAX_EFFECTS_PER_SIDE.get();
        List<Tradeoff.ModifierSpec> pool = candidates(positive);
        List<Tradeoff.ModifierSpec> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int remaining = budget;

        while (result.size() < minCount) {
            final int r = remaining;
            List<Tradeoff.ModifierSpec> fitting = pool.stream()
                    .filter(x -> !used.contains(x.stableKey()))
                    .filter(x -> x.weight() <= r)
                    .toList();
            if (fitting.isEmpty()) break;
            Tradeoff.ModifierSpec picked = weightedPick(random, fitting);
            result.add(picked); used.add(picked.stableKey()); remaining -= picked.weight();
        }

        while (result.size() < maxCount && remaining > 0) {
            final int r = remaining;
            List<Tradeoff.ModifierSpec> fitting = pool.stream()
                    .filter(x -> !used.contains(x.stableKey()))
                    .filter(x -> x.weight() <= r)
                    .toList();
            if (fitting.isEmpty()) break;
            Tradeoff.ModifierSpec picked = weightedPick(random, fitting);
            result.add(picked); used.add(picked.stableKey()); remaining -= picked.weight();
            if (random.nextFloat() < 0.12f && result.size() >= minCount) break;
        }

        if (result.isEmpty() && !pool.isEmpty()) result.add(pool.get(random.nextInt(pool.size())));
        return result;
    }

    private static Tradeoff.ModifierSpec weightedPick(Random random, List<Tradeoff.ModifierSpec> pool) {
        double total = 0;
        for (Tradeoff.ModifierSpec s : pool) total += 1.0 / Math.max(1, s.weight());
        double roll = random.nextDouble() * total;
        for (Tradeoff.ModifierSpec s : pool) {
            roll -= 1.0 / Math.max(1, s.weight());
            if (roll <= 0) return s;
        }
        return pool.get(pool.size() - 1);
    }

    private static List<Tradeoff.ModifierSpec> candidates(boolean positive) {
        List<Tradeoff.ModifierSpec> out = new ArrayList<>();
        boolean cte2 = isCraftToExile2Environment();

        if (TTConfig.ENABLE_VANILLA_EFFECTS.get()) {
            for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!"minecraft".equals(id.getNamespace())) continue;
                MobEffectCategory cat = entry.getValue().getCategory();
                if ((positive && cat != MobEffectCategory.BENEFICIAL) || (!positive && cat != MobEffectCategory.HARMFUL)) continue;
                addEffectVariants(out, id, VANILLA_WEIGHTS.getOrDefault(id.getPath(), 40));
            }
        }

        if (TTConfig.ENABLE_MODDED_EFFECTS.get() || cte2) {
            for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if ("minecraft".equals(id.getNamespace())) continue;
                if (!cte2 && !TTConfig.ENABLE_MODDED_EFFECTS.get()) continue;
                MobEffectCategory cat = entry.getValue().getCategory();
                if ((positive && cat != MobEffectCategory.BENEFICIAL) || (!positive && cat != MobEffectCategory.HARMFUL)) continue;
                addEffectVariants(out, id, moddedEffectWeight(id));
            }
        }

        if (TTConfig.ENABLE_VANILLA_ATTRIBUTES.get()) {
            for (var entry : BuiltInRegistries.ATTRIBUTE.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if ("minecraft".equals(id.getNamespace()) && isVanillaPlayerAttribute(id)) {
                    addAttributeVariants(out, id, positive);
                }
            }
        }

        if (TTConfig.ENABLE_MODDED_ATTRIBUTES.get() || cte2) {
            for (var entry : BuiltInRegistries.ATTRIBUTE.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if (!"minecraft".equals(id.getNamespace()) && (cte2 || TTConfig.ENABLE_MODDED_ATTRIBUTES.get())) {
                    addAttributeVariants(out, id, positive);
                }
            }
        }

        if (cte2) {
            addMineAndSlashStats(out, positive);
        }

        return out;
    }

    private static void addEffectVariants(List<Tradeoff.ModifierSpec> out, ResourceLocation id, int baseWeight) {
        int[] amps = {0, 1, 2};
        int[] costs = {baseWeight, Math.max(baseWeight + 15, baseWeight * 2), Math.max(baseWeight + 30, baseWeight * 3)};
        for (int i = 0; i < amps.length; i++) {
            out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.MOB_EFFECT, id, amps[i], 0,
                    AttributeModifier.Operation.ADDITION, Math.min(500, costs[i])));
        }
    }

    private static void addAttributeVariants(List<Tradeoff.ModifierSpec> out, ResourceLocation id, boolean positive) {
        double[] amounts = {0.03, 0.07, 0.12, 0.20, 0.30};
        int[] weights = {20, 50, 100, 180, 300};
        for (int i = 0; i < amounts.length; i++) {
            double amount = positive ? amounts[i] : -amounts[i];
            out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.ATTRIBUTE, id, 0, amount,
                    AttributeModifier.Operation.MULTIPLY_BASE, weights[i]));
        }
    }

    private static void addMineAndSlashStats(List<Tradeoff.ModifierSpec> out, boolean positive) {
        for (MnsStatCatalog.Descriptor stat : MnsStatCatalog.all()) {
            double[] values = MnsStatCatalog.values(stat);
            for (int i = 0; i < values.length; i++) {
                double amount = positive ? values[i] : -values[i];
                int weight = MnsStatCatalog.valueWeight(stat, i);
                // MnS uses FLAT for stats that represent a percentage itself
                // and for direct resource/stat points. This is the same
                // convention used by its own StatMod.percent/no-scaling logic.
                out.add(new Tradeoff.ModifierSpec(
                        Tradeoff.ModifierType.MNS_STAT,
                        stat.id(),
                        0,
                        amount,
                        AttributeModifier.Operation.ADDITION,
                        weight,
                        "FLAT"
                ));
            }
        }
    }

    private static boolean isVanillaPlayerAttribute(ResourceLocation id) {
        String p = id.getPath();
        return p.equals("generic.max_health") || p.equals("generic.follow_range") || p.equals("generic.knockback_resistance")
                || p.equals("generic.movement_speed") || p.equals("generic.flying_speed") || p.equals("generic.attack_damage")
                || p.equals("generic.attack_knockback") || p.equals("generic.attack_speed") || p.equals("generic.armor")
                || p.equals("generic.armor_toughness") || p.equals("generic.luck") || p.equals("generic.reach_distance");
    }

    private static int moddedEffectWeight(ResourceLocation id) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        if (p.contains("wither") || p.contains("wound") || p.contains("bleed")) return 250;
        if (p.contains("regen") || p.contains("barrier") || p.contains("fortify")) return 180;
        if (p.contains("burn") || p.contains("poison") || p.contains("venom")) return 160;
        if (p.contains("blind") || p.contains("weakness") || p.contains("slow")) return 80;
        return 100;
    }

    private EffectCatalog() {}
}
