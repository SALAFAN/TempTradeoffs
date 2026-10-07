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
    private static volatile List<Tradeoff.ModifierSpec> positiveCache;
    private static volatile List<Tradeoff.ModifierSpec> negativeCache;

    static {
        // Default balance is tuned around a 1000-point side budget.
        // Lower values are cheaper/common; higher values represent stronger effects.
        VANILLA_WEIGHTS.put("speed", 90); VANILLA_WEIGHTS.put("slowness", 90);
        VANILLA_WEIGHTS.put("haste", 95); VANILLA_WEIGHTS.put("mining_fatigue", 110);
        VANILLA_WEIGHTS.put("strength", 210); VANILLA_WEIGHTS.put("weakness", 150);
        VANILLA_WEIGHTS.put("jump_boost", 80); VANILLA_WEIGHTS.put("nausea", 120);
        VANILLA_WEIGHTS.put("regeneration", 300); VANILLA_WEIGHTS.put("resistance", 300);
        VANILLA_WEIGHTS.put("fire_resistance", 220); VANILLA_WEIGHTS.put("water_breathing", 100);
        VANILLA_WEIGHTS.put("invisibility", 280); VANILLA_WEIGHTS.put("blindness", 210);
        VANILLA_WEIGHTS.put("night_vision", 110); VANILLA_WEIGHTS.put("hunger", 130);
        VANILLA_WEIGHTS.put("poison", 240); VANILLA_WEIGHTS.put("wither", 400);
        VANILLA_WEIGHTS.put("absorption", 180); VANILLA_WEIGHTS.put("saturation", 220);
        VANILLA_WEIGHTS.put("glowing", 90); VANILLA_WEIGHTS.put("levitation", 320);
        VANILLA_WEIGHTS.put("slow_falling", 100); VANILLA_WEIGHTS.put("conduit_power", 220);
        VANILLA_WEIGHTS.put("dolphins_grace", 100); VANILLA_WEIGHTS.put("bad_omen", 160);
        VANILLA_WEIGHTS.put("hero_of_the_village", 300); VANILLA_WEIGHTS.put("darkness", 230);
    }
    public static boolean isCraftToExile2Environment() {
        if (!TTConfig.ENABLE_CTE2_INTEGRATION.get()) return false;
        // Mine and Slash is registered as "mmorpg" in the CTE2 environment.
        return ModList.get().isLoaded(MINE_AND_SLASH)
                && (ModList.get().isLoaded("exile_overlay") || ModList.get().isLoaded("library_of_exile"));
    }

    public static List<Tradeoff.ModifierSpec> positive(Random random) { return buildSide(random, true); }
    public static List<Tradeoff.ModifierSpec> negative(Random random) { return buildSide(random, false); }

    public static void invalidateCache() {
        positiveCache = null;
        negativeCache = null;
    }

    private static List<Tradeoff.ModifierSpec> buildSide(Random random, boolean positive) {
        int budget = positive
                ? Math.min(TTConfig.POSITIVE_WEIGHT_BUDGET.get(), TTConfig.MAX_SELECTION_WEIGHT.get())
                : Math.min(TTConfig.NEGATIVE_WEIGHT_BUDGET.get(), TTConfig.MAX_SELECTION_WEIGHT.get());
        int minCount = Math.min(TTConfig.MIN_EFFECTS_PER_SIDE.get(), TTConfig.MAX_EFFECTS_PER_SIDE.get());
        int maxCount = TTConfig.MAX_EFFECTS_PER_SIDE.get();

        List<Tradeoff.ModifierSpec> pool = candidatesCached(positive).stream()
                // Check enabled state and read overrides from the raw/default variant
                // before applying user-edited values/weights. This keeps the config key
                // stable when the player changes the numeric fields.
                .filter(x -> TTConfig.isModifierEnabled(x.stableKey(), positive))
                .map(x -> new Tradeoff.ModifierSpec(x.type(), x.id(), x.amplifier(),
                        TTConfig.getValueOverride(x.valueKey(), x.amount(), positive), x.operation(),
                        TTConfig.getWeightOverride(x.stableKey(), x.weight(), positive), x.mnsModType()))
                .filter(x -> x.weight() <= budget)
                .toList();

        if (pool.isEmpty()) return List.of();

        // Build several cheap randomized candidates and keep the one whose total
        // weight is closest to the budget. This makes a 1000-point budget mean
        // approximately 1000 points in the generated card instead of merely
        // acting as an upper limit. The work happens only when a card is created.
        List<Tradeoff.ModifierSpec> best = List.of();
        int bestTotal = -1;
        int attempts = Math.min(16, Math.max(6, pool.size() / 25));

        for (int attempt = 0; attempt < attempts; attempt++) {
            List<Tradeoff.ModifierSpec> candidate = new ArrayList<>();
            Set<String> used = new HashSet<>();
            int remaining = budget;

            while (candidate.size() < maxCount && remaining > 0) {
                final int r = remaining;
                List<Tradeoff.ModifierSpec> fitting = pool.stream()
                        .filter(x -> !used.contains(x.stableKey()))
                        .filter(x -> x.weight() <= r)
                        .toList();
                if (fitting.isEmpty()) break;

                // Near the end of the budget, favor options that consume most of
                // what remains. Earlier selections retain inverse-weight randomness.
                Tradeoff.ModifierSpec picked = budgetPick(random, fitting, remaining, candidate.size() >= minCount);
                candidate.add(picked);
                used.add(picked.stableKey());
                remaining -= picked.weight();

                if (candidate.size() >= minCount && remaining <= Math.max(10, budget / 100)) break;
            }

            if (candidate.size() < minCount) continue;
            int total = budget - remaining;
            if (total > bestTotal) {
                bestTotal = total;
                best = candidate;
            }
            if (total >= budget - Math.max(10, budget / 100)) break;
        }

        if (!best.isEmpty()) return best;
        return List.of(pool.get(random.nextInt(pool.size())));
    }

    private static Tradeoff.ModifierSpec budgetPick(Random random, List<Tradeoff.ModifierSpec> pool, int remaining, boolean mayFavorLarge) {
        double total = 0;
        for (Tradeoff.ModifierSpec s : pool) {
            double fit = mayFavorLarge ? 1.0 / (1.0 + Math.abs(remaining - s.weight())) : 1.0;
            double rarity = 1.0 / Math.sqrt(Math.max(1, s.weight()));
            total += fit * rarity;
        }
        double roll = random.nextDouble() * total;
        for (Tradeoff.ModifierSpec s : pool) {
            double fit = mayFavorLarge ? 1.0 / (1.0 + Math.abs(remaining - s.weight())) : 1.0;
            double rarity = 1.0 / Math.sqrt(Math.max(1, s.weight()));
            roll -= fit * rarity;
            if (roll <= 0) return s;
        }
        return pool.get(pool.size() - 1);
    }

    public static List<Tradeoff.ModifierSpec> candidatesForConfig(boolean positive) {
        return candidatesCached(positive);
    }

    private static List<Tradeoff.ModifierSpec> candidatesCached(boolean positive) {
        List<Tradeoff.ModifierSpec> cached = positive ? positiveCache : negativeCache;
        if (cached != null) return cached;
        synchronized (EffectCatalog.class) {
            cached = positive ? positiveCache : negativeCache;
            if (cached == null) {
                cached = List.copyOf(candidates(positive));
                if (positive) positiveCache = cached; else negativeCache = cached;
            }
            return cached;
        }
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
        int[] weights = {70, 140, 240, 360, 500};
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
        if (p.contains("wither") || p.contains("wound") || p.contains("bleed")) return 360;
        if (p.contains("regen") || p.contains("barrier") || p.contains("fortify")) return 270;
        if (p.contains("burn") || p.contains("poison") || p.contains("venom")) return 250;
        if (p.contains("blind") || p.contains("weakness") || p.contains("slow")) return 160;
        return 190;
    }

    private EffectCatalog() {}
}
