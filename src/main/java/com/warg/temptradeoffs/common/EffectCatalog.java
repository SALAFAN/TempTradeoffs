package com.warg.temptradeoffs.common;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;

public final class EffectCatalog {
    private static final Map<String, Integer> VANILLA_WEIGHTS = new HashMap<>();
    /** Exact positive status-effect policy requested for the current effect pool.
     * Keys are matched against registry path/description-id aliases so the policy
     * remains usable across namespaces used by different modpacks. */
    private static final List<PositiveRule> POSITIVE_RULES = new ArrayList<>();
    // These are the grouped I/II/... families. Their internal order is not
    // the UI priority: paired families are sorted first in the configuration UI,
    // then other multi-variant families, and only after that ungrouped effects.
    private static final List<String> SPLIT_FAMILIES = List.of(
            "Алкоголь", "Буйство", "Газированные", "Жареное", "Зерно", "Лёд",
            "Молочные", "Мясо", "Овощи", "Рыба", "Сахар", "Специи",
            "Странные", "Чай", "Фрукты"
    );
    private static volatile List<Tradeoff.ModifierSpec> positiveCache;

    private record PositiveRule(String[] aliases, int[] levels, int[] weights, boolean removed, String family) {
        boolean matches(String haystack) {
            String normalizedHaystack = normalizeAliasText(haystack);
            for (String alias : aliases) {
                if (haystack.contains(alias)) return true;
                String normalizedAlias = normalizeAliasText(alias);
                if (!normalizedAlias.isEmpty() && normalizedHaystack.contains(normalizedAlias)) return true;
            }
            return false;
        }
    }
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

        // Positive pool explicitly requested by the user. The first matching rule
        // wins. Removed effects are excluded completely; custom levels/weights are
        // used instead of the generic potion-derived variants.
        rule(false, "bloodlust", new int[]{5,10,50}, new int[]{200,400,1000}, "Bloodlust");
        rule(true,  "moons_curse", null, null, null);
        rule(false, "geomance", new int[]{1}, new int[]{150}, "Geomance");
        rule(false, "harmonic_flow", new int[]{1}, new int[]{300}, "Harmonic flow");
        rule(true,  "lich_charge", null, null, null);
        rule(true,  "lost_items", null, null, null);
        rule(false, "poison_resistance", new int[]{1}, new int[]{450}, "Poison resistance");
        rule(false, "rage", new int[]{5,10,50}, new int[]{200,400,1000}, "Rage");
        rule(false, "suns_blessing", new int[]{1}, new int[]{150}, "Sun's Blessing");
        rule(true,  "sunblock", null, null, null);
        rule(false, "twilight_aura", new int[]{1}, new int[]{250}, "Twilight aura");
        rule(false, "alcohol", new int[]{1,2}, new int[]{250,350}, "Алкоголь");
        rule(false, "crimson_shadow|crimsonshade|crimson_shadow_effect", new int[]{1}, new int[]{350}, "Багровой тени");
        rule(true, "frenzied|ferocity", null, null, null);
        rule(false, "frenzy|light_frenzy|frenzy_light|mild_frenzy|frenzy_mild|moderate_frenzy|frenzy_moderate|medium_frenzy|frenzy_medium|strong_frenzy|frenzy_strong|unrestrained_frenzy|frenzy_unrestrained|unbridled_frenzy|frenzy_unbridled|furious_frenzy|frenzy_furious", new int[]{1,2,3,4,5}, new int[]{300,450,600,750,900}, "Буйство");
        rule(true, "tiger_blessing|tigers_blessing", null, null, null);
        rule(false, "divine_shield", new int[]{1}, new int[]{500}, "Божественный щит");
        rule(true, "food_bonus|food_bonuses", null, null, null);
        rule(true, "potion_bonus|potion_bonuses", null, null, null);
        rule(true, "seafood_bonus|seafood_bonuses", null, null, null);
        rule(true, "enderman_burden|burden_of_enderman", null, null, null);
        rule(true, "bubbling_vitality|bubbling_life_force", null, null, null);
        rule(false, "light_footed|lightfooted", new int[]{1}, new int[]{200}, "Быстроногий");
        rule(false, "luck", new int[]{5,10,50}, new int[]{200,400,1000}, "Везение");
        rule(false, "carbonated", new int[]{1,2}, new int[]{250,350}, "Газированные");
        rule(false, "hero_of_the_village", new int[]{1}, new int[]{300}, "Герой деревни");
        rule(false, "dolphins_grace", new int[]{1}, new int[]{100}, "Грация дельфина");
        rule(false, "day_on_the_moon", new int[]{1}, new int[]{400}, "День на луне");
        rule(false, "fried", new int[]{1,2}, new int[]{250,350}, "Жареное");
        rule(true, "bug_pheromones|insect_pheromones", null, null, null);
        rule(false, "hardened_mouth|hardenedmouth", new int[]{1}, new int[]{100}, "Закалённый рот");
        rule(true, "charged_strike|charged_hit", null, null, null);
        rule(false, "insect_protection|protection_from_insects", new int[]{1}, new int[]{150}, "Защита от насекомых");
        rule(false, "grain", new int[]{1,2}, new int[]{250,350}, "Зерно");
        rule(false, "knowledge_of_ages|knowledge_of_the_ages", new int[]{1}, new int[]{300}, "Знание веков");
        rule(false, "knowledge", new int[]{1}, new int[]{50}, "Знания");
        rule(false, "poison_immunity|immunity_to_poison", new int[]{1}, new int[]{300}, "Иммунитет к яду");
        rule(false, "healing", new int[]{5,10,50}, new int[]{200,400,1000}, "Исцеление");
        rule(false, "sticky_touch|sticky_fingers", new int[]{1}, new int[]{150}, "Клейкое прикосновение");
        rule(false, "combo_lunch|combo_meal", new int[]{1}, new int[]{250}, "Комбо-обед");
        rule(false, "comfort", new int[]{1}, new int[]{500}, "Комфорт");
        rule(true, "bone_barrier", null, null, null);
        rule(false, "caffeine_charge", new int[]{1}, new int[]{400}, "Кофеиновый заряд");
        rule(false, "soul_steal", new int[]{1}, new int[]{400}, "Кража души");
        rule(false, "strong_armor|reinforced_armor", new int[]{1}, new int[]{400}, "Крепкая броня");
        rule(false, "lava_vision|lava_sight", new int[]{1}, new int[]{100}, "Лавовое видение");
        rule(false, "ice_barrier|frost_barrier", new int[]{1}, new int[]{450}, "Ледяной барьер");
        rule(false, "lightness|weightlessness", new int[]{1}, new int[]{200}, "Лёгкость");
        rule(false, "ice", new int[]{1,2}, new int[]{250,350}, "Лёд");
        rule(false, "oily", new int[]{1}, new int[]{100}, "Масленый");
        rule(true, "instant_arrows", null, null, null);
        rule(false, "milky", new int[]{1,2}, new int[]{250,350}, "Молочные");
        rule(false, "frost_field|frosty_field", new int[]{1}, new int[]{450}, "Морозное поле");
        rule(false, "sea_charm|marine_charm", new int[]{1}, new int[]{100}, "Морского очарования");
        rule(false, "orca_power|killer_whale_power", new int[]{1}, new int[]{300}, "Мощь косатки");
        rule(false, "meat", new int[]{1,2}, new int[]{250,350}, "Мясо");
        rule(false, "saturation", new int[]{1}, new int[]{350}, "Насыщенность");
        rule(true, "onslaught", null, null, null);
        rule(true, "natto_bite", null, null, null);
        rule(false, "invisibility", new int[]{1,2,3}, new int[]{400,700,1000}, "Невидимость");
        rule(false, "untouchable|untouchability", new int[]{1}, new int[]{500}, "Неприкасаемый");
        rule(false, "night_vision", new int[]{1}, new int[]{500}, "Ночное зрение");
        rule(false, "vegetables", new int[]{1,2}, new int[]{250,350}, "Овощи");
        rule(false, "fire_field|flame_field", new int[]{1}, new int[]{500}, "Огненное поле");
        rule(false, "fire_resistance", new int[]{1}, new int[]{500}, "Огнестойкость");
        rule(false, "phoenix_enlightenment|phoenix_illumination", new int[]{1}, new int[]{400}, "Озарение Жар-птицы");
        rule(false, "mosquito_repellent|mosquito_repellence", new int[]{1}, new int[]{150}, "Отпугивание комаров");
        rule(false, "reach", new int[]{2,4,6}, new int[]{200,400,800}, "Охват");
        rule(false, "floating_legs|levitating_legs", new int[]{1}, new int[]{250}, "Парящих ног");
        rule(false, "slow_falling", new int[]{1}, new int[]{250}, "Плавное падение");
        rule(false, "absorption", new int[]{2,4,6}, new int[]{200,350,500}, "Поглощение");
        rule(true, "flight|flying", null, null, null);
        rule(false, "intermittent_silence|periodic_silence", new int[]{1}, new int[]{200}, "Прерывистое безмолвие");
        rule(false, "vampire_touch|vampiric_touch", new int[]{1}, new int[]{500}, "Прикосновение вампира");
        rule(true, "enigmatic_dice|health_surge_enigmatic_dice", null, null, null);
        rule(false, "vanilla_health_surge|health_surge_vanilla", new int[]{2,4,6}, new int[]{200,350,500}, "Прилив здоровья Ванильный");
        rule(false, "rabbit_agility|rabbit_swiftness", new int[]{1}, new int[]{400}, "Проворство кролика");
        rule(false, "jump_boost", new int[]{2,4,6,8,10}, new int[]{200,350,500,650,800}, "Прыгучесть");
        rule(false, "regeneration", new int[]{2,4,6,8}, new int[]{200,400,800,1000}, "Регенерация");
        rule(false, "fish", new int[]{1,2}, new int[]{250,350}, "Рыба");
        rule(false, "sugar", new int[]{1,2}, new int[]{250,350}, "Сахар");
        rule(false, "balanced", new int[]{1}, new int[]{850}, "Сбалансированный");
        rule(false, "free_breathing|free_breath", new int[]{1}, new int[]{300}, "Свободное дыхание");
        rule(false, "strength", new int[]{2,4,6,9,12,15}, new int[]{150,300,450,600,750,900}, "Сила");
        rule(false, "source_power|power_of_source", new int[]{1}, new int[]{350}, "Сила источника");
        rule(false, "sculk_pull|sculk_attraction", new int[]{1}, new int[]{200}, "Скалковое притяжение");
        rule(true, "aquamirae_speed|speed_from_aquamirae|aquamirae", null, null, null);
        rule(false, "vanilla_speed|speed", new int[]{1,2,3,4,5}, new int[]{200,350,500,650,800}, "Скорость Ванильный");
        rule(false, "soul_crossing|soul_crossover", new int[]{1}, new int[]{350}, "Скрещивания душ");
        rule(false, "sweet_heart|sweetheart", new int[]{1}, new int[]{250}, "Сладкое сердце");
        rule(false, "snowstorm|snow_storm", new int[]{1}, new int[]{450}, "Снежная буря");
        rule(false, "resistance", new int[]{1,2,3,4}, new int[]{200,450,700,950}, "Сопротивление");
        rule(false, "explosion_resistance|blast_resistance", new int[]{1}, new int[]{350}, "Сопротивление взрывам");
        rule(false, "infection_resistance|resistance_to_infection", new int[]{1}, new int[]{300}, "Сопротивление заражению");
        rule(false, "wither_resistance|resistance_to_wither", new int[]{1}, new int[]{300}, "Сопротивление иссушению");
        rule(false, "knockback_resistance_advanced|knockback_resistance_percent", new int[]{10,20,50}, new int[]{200,400,650}, "Сопротивление к отбрасыванию");
        rule(false, "knockback_resistance", new int[]{1,2}, new int[]{400,800}, "Сопротивление отбрасыванию");
        rule(false, "spices", new int[]{1,2}, new int[]{250,350}, "Специи");
        rule(false, "haste", new int[]{2,4,6,8,10}, new int[]{200,350,500,650,800}, "Спешка");
        rule(false, "stimulation", new int[]{1}, new int[]{250}, "Стимуляция");
        rule(false, "stone_sturdiness|stone_toughness", new int[]{1}, new int[]{200}, "Стойкость камня");
        rule(false, "strange", new int[]{1,2}, new int[]{250,350}, "Странные");
        rule(false, "predator_drive|predator_instinct", new int[]{1}, new int[]{250}, "Стремление хищника");
        rule(false, "saturation_food|satiety", new int[]{1}, new int[]{500}, "Сытость");
        rule(true, "dark_amulet", null, null, null);
        rule(false, "void_vanity|vanity_of_the_void", new int[]{1}, new int[]{400}, "Тщеславия пустоты");
        rule(false, "armor_increase|increased_armor", new int[]{1}, new int[]{450}, "Увеличение брони");
        rule(false, "poison_resistance_effect", new int[]{1}, new int[]{300}, "Устойчивость к ядам");
        rule(false, "fruits|fruit", new int[]{1,2}, new int[]{250,350}, "Фрукты");
        rule(false, "grappling|cling|clinging", new int[]{1}, new int[]{450}, "Цепляние");
        rule(false, "tea", new int[]{1,2}, new int[]{250,350}, "Чай");
        rule(false, "poison_field|venom_field", new int[]{1}, new int[]{450}, "Ядовитое поле");
        rule(false, "wrath|fury", new int[]{1}, new int[]{300}, "Ярость");
    }

    private static void rule(boolean removed, String aliases, int[] levels, int[] weights, String family) {
        POSITIVE_RULES.add(new PositiveRule(aliases.split("\\|"), levels, weights, removed, family));
    }
    public static boolean isCraftToExile2Environment() {
        return SourceCatalog.extendedPoolEnabled();
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
                .map(x -> withConfiguredValue(x, positive))
                .filter(x -> x.weight() <= budget)
                .toList();

        if (pool.isEmpty()) {
            // Never generate an empty side just because an older config contains
            // stale per-variant enable entries. Keep the card functional; explicit
            // disabling of every variant is not allowed because a card must have
            // at least one modifier on each side.
            pool = candidatesCached(positive).stream()
                    .map(x -> withConfiguredValue(x, positive))
                    .filter(x -> x.weight() <= budget)
                    .toList();
        }
        if (pool.isEmpty()) {
            ResourceLocation fallbackId = new ResourceLocation("minecraft", positive ? "speed" : "slowness");
            int fallbackAmp = 0;
            int fallbackWeight = Math.min(90, budget);
            Tradeoff.ModifierSpec fallback = new Tradeoff.ModifierSpec(
                    Tradeoff.ModifierType.MOB_EFFECT, fallbackId, fallbackAmp, 1,
                    AttributeModifier.Operation.ADDITION, Math.max(1, fallbackWeight));
            pool = List.of(fallback);
        }

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


    private static Tradeoff.ModifierSpec withConfiguredValue(Tradeoff.ModifierSpec x, boolean positive) {
        double value = TTConfig.getValueOverride(x.valueKey(),
                x.type() == Tradeoff.ModifierType.MOB_EFFECT ? x.amplifier() + 1 : x.amount(), positive);
        int amplifier = x.amplifier();
        double amount = value;
        if (x.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            int level = Math.max(1, Math.min(255, (int)Math.rint(value)));
            amplifier = level - 1;
            amount = level;
        }
        return new Tradeoff.ModifierSpec(x.type(), x.id(), amplifier, amount, x.operation(),
                TTConfig.getWeightOverride(x.stableKey(), x.weight(), positive), x.mnsModType());
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
        List<Tradeoff.ModifierSpec> out = new ArrayList<>(candidatesCached(positive));
        if (isCraftToExile2Environment()) {
            Set<String> seen = new HashSet<>();
            for (Tradeoff.ModifierSpec spec : out) seen.add(spec.stableKey());
            List<Tradeoff.ModifierSpec> extra = new ArrayList<>();
            SourceCatalog.collectAdditionalModifiers(extra, positive, true);
            for (Tradeoff.ModifierSpec spec : extra) {
                if (seen.add(spec.stableKey())) out.add(spec);
            }
        }
        return List.copyOf(out);
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
                if (isCriticalEffect(id, entry.getValue())) continue;
                addPositiveOrDefault(out, id, entry.getValue(), VANILLA_WEIGHTS.getOrDefault(id.getPath(), 40), positive);
            }
        }

        if (TTConfig.ENABLE_MODDED_EFFECTS.get() || cte2) {
            for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
                ResourceLocation id = entry.getKey().location();
                if ("minecraft".equals(id.getNamespace())) continue;
                if (!cte2 && !TTConfig.ENABLE_MODDED_EFFECTS.get()) continue;
                MobEffectCategory cat = entry.getValue().getCategory();
                if ((positive && cat != MobEffectCategory.BENEFICIAL) || (!positive && cat != MobEffectCategory.HARMFUL)) continue;
                if (isCriticalEffect(id, entry.getValue())) continue;
                addPositiveOrDefault(out, id, entry.getValue(), moddedEffectWeight(id), positive);
            }
        }

        if (positive) {
            addSplitFamilyVariants(out, cte2);
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
            SourceCatalog.collectAdditionalModifiers(out, positive, false);
        }

        return sanitizeCandidates(out, positive);
    }

    /**
     * Final safety gate for the two polarity pools. Classification earlier in
     * the pipeline is intentionally duplicated here so an integration or a
     * future catalog rule cannot accidentally leak a harmful MobEffect into the
     * positive pool (or a beneficial one into the negative pool).
     *
     * Exact duplicate specs are removed while different registry IDs remain
     * distinct; this is important for real split families such as I/II.
     */
    private static List<Tradeoff.ModifierSpec> sanitizeCandidates(List<Tradeoff.ModifierSpec> input, boolean positive) {
        LinkedHashMap<String, Tradeoff.ModifierSpec> unique = new LinkedHashMap<>();
        for (Tradeoff.ModifierSpec spec : input) {
            if (!isAllowedPolarity(spec, positive)) continue;
            unique.putIfAbsent(spec.stableKey(), spec);
        }
        return List.copyOf(unique.values());
    }

    private static boolean isAllowedPolarity(Tradeoff.ModifierSpec spec, boolean positive) {
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect == null || isCriticalEffect(spec.id(), effect)) return false;
            MobEffectCategory category = effect.getCategory();
            return positive ? category == MobEffectCategory.BENEFICIAL : category == MobEffectCategory.HARMFUL;
        }
        // Attributes and Mine & Slash stats are intentionally available on both
        // sides; the sign of the generated amount determines whether they are a
        // benefit or a penalty.
        return spec.type() == Tradeoff.ModifierType.ATTRIBUTE
                || spec.type() == Tradeoff.ModifierType.MNS_STAT;
    }

    /** Critical harmful effects are not part of TempTradeoffs at all. They are not
     * exposed in configuration and can never be generated as a card modifier. */
    private static boolean isCriticalEffect(ResourceLocation id, MobEffect effect) {
        String p = id.getPath().toLowerCase(Locale.ROOT);
        if (p.equals("instant_damage") || p.equals("poison") || p.equals("wither")) return true;
        if (p.contains("detonation") || p.contains("detonate") || p.contains("explosion")
                || p.contains("explode") || p.contains("instant_damage") || p.contains("instantdamage")
                || p.contains("self_damage") || p.contains("selfdamage") || p.contains("suicide")
                || p.contains("health_drain") || p.contains("healthdrain") || p.contains("bleed")
                || p.contains("hemorrhage") || p.contains("wound") || p.contains("agony")) return true;
        try {
            java.lang.reflect.Method m = MobEffect.class.getMethod("isInstantenous");
            if (Boolean.TRUE.equals(m.invoke(effect))) {
                return p.contains("damage") || p.contains("harm") || p.contains("deton");
            }
        } catch (Throwable ignored) {}
        String key = effect.getDescriptionId().toLowerCase(Locale.ROOT);
        return key.contains("detonation") || key.contains("detonate") || key.contains("instant_damage")
                || key.contains("self_damage") || key.contains("health_drain");
    }

    private static void addPositiveOrDefault(List<Tradeoff.ModifierSpec> out, ResourceLocation id, MobEffect effect, int baseWeight, boolean positive) {
        if (!positive) {
            addEffectVariants(out, id, baseWeight);
            return;
        }
        PositiveRule rule = positiveRule(id, effect);
        if (rule != null) {
            if (rule.removed()) return;
            // The food/drink families requested by the user consist of separate
            // registered effects (I, II, etc.), not one effect with multiple potion
            // amplifiers. They are added together below by addSplitFamilyVariants().
            if (SPLIT_FAMILIES.contains(rule.family())) return;
            for (int i = 0; i < rule.levels().length; i++) {
                int level = rule.levels()[i];
                int weight = rule.weights()[i];
                out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.MOB_EFFECT, id, level - 1, level,
                        AttributeModifier.Operation.ADDITION, weight));
            }
            return;
        }
        // For the positive pool, modded effects are opt-in: only effects explicitly
        // present in the user's requested positive list are allowed. Otherwise a
        // generic fallback weight (such as 190) would leak unrequested effects
        // into the configuration and into generated cards. Vanilla effects retain
        // their normal catalog fallback.
        if (!"minecraft".equals(id.getNamespace())) return;
        addEffectVariants(out, id, baseWeight);
    }

    /**
     * Builds the explicitly requested I/II/... families from the actual registered
     * effects. Each registered effect is represented exactly once: its ordinal
     * within the family becomes the visible level and receives that level's weight.
     * This prevents the old bug where every effect in a family was expanded into
     * every configured level, producing duplicate "ур. 1", "ур. 2", etc.
     */
    private static void addSplitFamilyVariants(List<Tradeoff.ModifierSpec> out, boolean cte2) {
        Map<String, List<Map.Entry<ResourceLocation, MobEffect>>> families = new LinkedHashMap<>();
        for (var entry : BuiltInRegistries.MOB_EFFECT.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            MobEffect effect = entry.getValue();
            if (isCriticalEffect(id, effect)) continue;
            if (!"minecraft".equals(id.getNamespace()) && !TTConfig.ENABLE_MODDED_EFFECTS.get() && !cte2) continue;
            PositiveRule rule = positiveRule(id, effect);
            if (rule == null || rule.removed() || !SPLIT_FAMILIES.contains(rule.family())) continue;
            families.computeIfAbsent(rule.family(), k -> new ArrayList<>()).add(Map.entry(id, effect));
        }

        for (var familyEntry : families.entrySet()) {
            String family = familyEntry.getKey();
            List<Map.Entry<ResourceLocation, MobEffect>> effects = familyEntry.getValue();
            PositiveRule rule = positiveRule(effects.get(0).getKey(), effects.get(0).getValue());
            if (rule == null || rule.levels() == null || rule.weights() == null) continue;

            // A family may be represented either by several registered effects
            // (e.g. "Фрукты I" + "Фрукты II") or by one effect with several
            // actual amplifiers. Handle both cases without duplicating levels.
            if (effects.size() == 1) {
                ResourceLocation id = effects.get(0).getKey();
                int count = Math.min(rule.levels().length, rule.weights().length);
                for (int i = 0; i < count; i++) {
                    int level = rule.levels()[i];
                    out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.MOB_EFFECT, id, level - 1, level,
                            AttributeModifier.Operation.ADDITION, rule.weights()[i]));
                }
                continue;
            }

            // Several registered effects: each effect is exactly one family
            // variant. For Frenzy/Bujstvo we explicitly keep five variants and
            // assign them to the five requested names/weights. Other split
            // families use their I/II/... suffixes and deterministic registry order.
            effects.sort(Comparator
                    .comparingInt((Map.Entry<ResourceLocation, MobEffect> e) -> familyVariantRank(family, e.getKey(), e.getValue()))
                    .thenComparing(e -> e.getKey().toString()));

            int maxVariants = Math.min(effects.size(), Math.min(rule.levels().length, rule.weights().length));
            if ("Буйство".equals(family)) {
                // Prefer uniquely identified ranks 1..5. If the source mod uses
                // opaque registry keys, the deterministic sorted order below still
                // gives exactly five variants rather than leaking them into the
                // general list as separate effects.
                maxVariants = Math.min(5, maxVariants);
            }

            Set<Integer> usedOrdinals = new HashSet<>();
            for (int i = 0; i < maxVariants; i++) {
                Map.Entry<ResourceLocation, MobEffect> e = effects.get(i);
                int ordinal = familyVariantRank(family, e.getKey(), e.getValue());
                if (ordinal < 1 || ordinal > maxVariants || usedOrdinals.contains(ordinal)) ordinal = i + 1;
                usedOrdinals.add(ordinal);
                int level = rule.levels()[ordinal - 1];
                int weight = rule.weights()[ordinal - 1];
                out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.MOB_EFFECT, e.getKey(), level - 1, level,
                        AttributeModifier.Operation.ADDITION, weight));
            }
        }
    }

    private static int familyVariantRank(String family, ResourceLocation id, MobEffect effect) {
        String hay = (id.getPath() + "|" + id + "|" + effect.getDescriptionId()).toLowerCase(Locale.ROOT);
        if ("Буйство".equals(family)) {
            // Exact ordered variants requested by the user.
            if (containsAny(hay, "light_frenzy", "frenzy_light", "light_berserk", "easy_frenzy", "mild_frenzy", "frenzy_1", "frenzy_i", "_light", "_mild")) return 1;
            if (containsAny(hay, "moderate_frenzy", "frenzy_moderate", "medium_frenzy", "frenzy_medium", "frenzy_2", "frenzy_ii", "_moderate", "_medium")) return 2;
            if (containsAny(hay, "strong_frenzy", "frenzy_strong", "frenzy_3", "frenzy_iii", "_strong")) return 3;
            if (containsAny(hay, "unrestrained_frenzy", "frenzy_unrestrained", "unbridled_frenzy", "frenzy_4", "frenzy_iv", "_unrestrained", "_unbridled", "_berserk")) return 4;
            if (containsAny(hay, "furious_frenzy", "frenzy_furious", "frenzy_5", "frenzy_v", "_furious")) return 5;
        }
        for (int n = 1; n <= 10; n++) {
            if (hay.matches(".*(?:_|-|\\b)" + n + "(?:\\b|$).*")) return n;
        }
        String[] roman = {"i","ii","iii","iv","v","vi","vii","viii","ix","x"};
        for (int i = 0; i < roman.length; i++) {
            if (hay.matches(".*(?:_|-|\\b)" + roman[i] + "(?:\\b|$).*")) return i + 1;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean containsAny(String hay, String... tokens) {
        for (String token : tokens) if (hay.contains(token)) return true;
        return false;
    }

    private static String normalizeAliasText(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static PositiveRule positiveRule(ResourceLocation id, MobEffect effect) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String desc = effect.getDescriptionId().toLowerCase(Locale.ROOT);
        String hay = id.toString().toLowerCase(Locale.ROOT) + "|" + path + "|" + desc;
        for (PositiveRule rule : POSITIVE_RULES) {
            if ("Скорость Ванильный".equals(rule.family()) && !"minecraft".equals(id.getNamespace())) continue;
            if ("Буйство".equals(rule.family())) {
                // The five Frenzy effects are separate registered effects in the
                // source mod. Their registry/translation keys are not guaranteed
                // to use exactly the same suffix, so identify the family first and
                // determine the exact variant later with familyVariantRank().
                if (isFrenzyFamilyId(hay)) return rule;
                continue;
            }
            if (rule.matches(hay)) return rule;
        }
        return null;
    }

    private static boolean isFrenzyFamilyId(String hay) {
        // Deliberately do not use the generic words "rage" or "ferocity":
        // those are separate configured effects (Ярость / Неистовость).
        return hay.contains("frenzy")
                || hay.contains("frenzied")
                || hay.contains("rampage")
                || hay.contains("berserk")
                || hay.contains("unbridled_fury")
                || hay.contains("light_frenzy")
                || hay.contains("mild_frenzy")
                || hay.contains("moderate_frenzy")
                || hay.contains("strong_frenzy")
                || hay.contains("furious_frenzy");
    }

    /** Returns a stable tie-break order for grouped status-effect families.
     * UI priority is determined by variant count first, not by this list. */
    public static int statusFamilyOrder(String family) {
        if (family == null) return Integer.MAX_VALUE;
        int index = SPLIT_FAMILIES.indexOf(family);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    public static boolean isSplitFamily(String family) {
        return family != null && SPLIT_FAMILIES.contains(family);
    }

    public static String statusFamilyKey(ResourceLocation id, boolean positive) {
        if (!positive) return "id:" + id;
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        PositiveRule rule = effect == null ? null : positiveRule(id, effect);
        return rule != null && rule.family() != null ? "profile:" + rule.family() : "id:" + id;
    }

    public static String statusFamilyName(ResourceLocation id, boolean positive) {
        if (!positive) return null;
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        PositiveRule rule = effect == null ? null : positiveRule(id, effect);
        return rule == null ? null : rule.family();
    }

    private static void addEffectVariants(List<Tradeoff.ModifierSpec> out, ResourceLocation id, int baseWeight) {
        SortedSet<Integer> levels = registeredEffectLevels(id);
        if (levels.isEmpty()) levels.add(1);

        int index = 0;
        for (int level : levels) {
            int cost = baseWeight;
            for (int i = 0; i < index; i++) cost = Math.max(cost + 15, cost * 2);
            out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.MOB_EFFECT, id, level - 1, level,
                    AttributeModifier.Operation.ADDITION, Math.min(500, cost)));
            index++;
        }
    }

    private static SortedSet<Integer> registeredEffectLevels(ResourceLocation effectId) {
        SortedSet<Integer> levels = new TreeSet<>();
        for (var entry : BuiltInRegistries.POTION.entrySet()) {
            for (MobEffectInstance instance : entry.getValue().getEffects()) {
                if (effectId.equals(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect()))) {
                    levels.add(instance.getAmplifier() + 1);
                }
            }
        }
        return levels;
    }

    private static void addAttributeVariants(List<Tradeoff.ModifierSpec> out, ResourceLocation id, boolean positive) {
        // Every attribute has exactly three configurable variants: 2, 5 and 10.
        // Attributes are direct numeric modifiers, so use ADDITION rather than
        // MULTIPLY_BASE; negative cards receive -2, -5 and -10.
        double[] amounts = {2.0, 5.0, 10.0};
        int[] weights = {120, 260, 500};
        for (int i = 0; i < amounts.length; i++) {
            double amount = positive ? amounts[i] : -amounts[i];
            out.add(new Tradeoff.ModifierSpec(Tradeoff.ModifierType.ATTRIBUTE, id, 0, amount,
                    AttributeModifier.Operation.ADDITION, weights[i]));
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
