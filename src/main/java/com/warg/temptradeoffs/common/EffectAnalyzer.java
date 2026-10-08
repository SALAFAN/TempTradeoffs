package com.warg.temptradeoffs.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Locale;

/**
 * Mod-agnostic automatic evaluator. It deliberately does not depend on a mod's
 * namespace: it derives a semantic domain from registry/translation text and
 * then evaluates magnitude, operation and (for potion effects) level.
 *
 * Unknown mechanics are rejected instead of being guessed into the pool.
 */
public final class EffectAnalyzer {
    private static final double MIN_CONFIDENCE = 0.70;
    private static final double LIMITED_CONFIDENCE = 0.58;
    private static final double MAX_AUTO_POWER = 900.0;

    private EffectAnalyzer() {}

    public static EffectAnalysis analyze(Tradeoff.ModifierSpec spec, boolean positive) {
        if (spec == null || spec.id() == null) return excluded("missing modifier identity");
        return switch (spec.type()) {
            case MOB_EFFECT -> analyzeMobEffect(spec, positive);
            case ATTRIBUTE -> analyzeAttribute(spec);
            case MNS_STAT -> analyzeMns(spec);
        };
    }

    public static boolean autoUsable(Tradeoff.ModifierSpec spec, boolean positive) {
        EffectAnalysis a = analyze(spec, positive);
        return a.decision() != EffectAnalysis.Decision.EXCLUDED
                && a.confidence() >= MIN_CONFIDENCE && a.power() <= MAX_AUTO_POWER;
    }

    public static int suggestedWeight(Tradeoff.ModifierSpec spec, boolean positive) {
        return Math.max(1, Math.min(950, analyze(spec, positive).suggestedWeight()));
    }

    /** Returns sensible candidate amounts for a numeric attribute based on its semantic domain. */
    public static double[] attributeValues(ResourceLocation id) {
        Semantic s = classify(semanticText(id, id.getPath()), "minecraft".equals(id.getNamespace()));
        return switch (s.domain) {
            case MOBILITY -> new double[]{0.01, 0.02, 0.05};
            case ATTACK_SPEED -> new double[]{0.05, 0.10, 0.20};
            case CRIT -> new double[]{0.05, 0.10, 0.20};
            case HEALTH -> new double[]{2.0, 5.0, 10.0};
            case DAMAGE -> new double[]{1.0, 3.0, 6.0};
            case DEFENSE, RESISTANCE -> new double[]{1.0, 3.0, 6.0};
            default -> new double[]{1.0, 2.0, 5.0};
        };
    }

    private static EffectAnalysis analyzeMobEffect(Tradeoff.ModifierSpec spec, boolean positive) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(spec.id());
        if (effect == null) return excluded("MobEffect is not registered");
        MobEffectCategory category = effect.getCategory();
        if (positive && category != MobEffectCategory.BENEFICIAL) return excluded("not beneficial");
        if (!positive && category != MobEffectCategory.HARMFUL) return excluded("not harmful");

        String text = semanticText(spec.id(), effect.getDescriptionId());
        if (isDangerous(text)) return excluded("dangerous mechanic");
        Semantic semantic = classify(text, "minecraft".equals(spec.id().getNamespace()));
        int level = Math.max(1, spec.amplifier() + 1);
        double scale = 1.0 + 0.65 * log2(level);
        double power = clamp(semantic.basePower * scale, 1.0, 1000.0);
        double confidence = semantic.confidence;
        if ("minecraft".equals(spec.id().getNamespace())) confidence = Math.max(confidence, 0.92);
        if (level > 5) confidence -= 0.05;
        return result(decide(power, confidence), semantic.domain, power, confidence,
                semantic.reason + ", level=" + level);
    }

    private static EffectAnalysis analyzeAttribute(Tradeoff.ModifierSpec spec) {
        Semantic semantic = classify(semanticText(spec.id(), spec.id().getPath()),
                "minecraft".equals(spec.id().getNamespace()));
        double amount = Math.abs(spec.amount());
        if (amount <= 0.0) return excluded("zero attribute amount");
        double reference = attributeReference(semantic.domain, spec.operation());
        double magnitude = reference <= 0.0 ? 1.0 : Math.sqrt(amount / reference);
        double operationFactor = switch (spec.operation()) {
            case ADDITION -> 1.0;
            case MULTIPLY_BASE -> 1.25;
            case MULTIPLY_TOTAL -> 1.50;
        };
        double power = clamp(semantic.basePower * magnitude * operationFactor, 1.0, 1000.0);
        double confidence = semantic.confidence;
        if (semantic.domain == EffectAnalysis.Domain.UNKNOWN) confidence = Math.min(confidence, 0.55);
        return result(decide(power, confidence), semantic.domain, power, confidence,
                semantic.reason + ", amount=" + amount + ", operation=" + spec.operation());
    }

    private static EffectAnalysis analyzeMns(Tradeoff.ModifierSpec spec) {
        String description = "";
        for (MnsStatCatalog.Descriptor d : MnsStatCatalog.all()) {
            if (d.id().equals(spec.id())) {
                description = d.name() + " " + d.description();
                break;
            }
        }
        Semantic semantic = classify(semanticText(spec.id(), spec.id().getPath() + " " + description), false);
        double amount = Math.abs(spec.amount());
        double reference = mnsReference(semantic.domain, description, amount);
        double magnitude = reference <= 0.0 ? 1.0 : Math.sqrt(amount / reference);
        double power = clamp(semantic.basePower * magnitude, 1.0, 1000.0);
        double confidence = semantic.confidence - (description.isBlank() ? 0.10 : 0.0);
        if (semantic.domain == EffectAnalysis.Domain.UNKNOWN) confidence = Math.min(confidence, 0.55);
        return result(decide(power, confidence), semantic.domain, power, confidence,
                semantic.reason + ", amount=" + amount);
    }

    private static Semantic classify(String text, boolean vanilla) {
        String t = text.toLowerCase(Locale.ROOT);
        if (contains(t, "critical", "crit_damage", "crit_chance"))
            return s(EffectAnalysis.Domain.CRIT, 520, vanilla, "critical mechanic");
        if (contains(t, "damage", "attack_damage", "strength", "power", "weapon_damage", "physical_damage"))
            return s(EffectAnalysis.Domain.DAMAGE, 650, vanilla, "damage mechanic");
        if (contains(t, "regeneration", "regen", "healing", "heal", "life_steal", "lifesteal", "vampire", "vampir"))
            return s(EffectAnalysis.Domain.REGENERATION, 620, vanilla, "healing/regeneration mechanic");
        if (contains(t, "max_health", "health", "heart", "vitality", "life"))
            return s(EffectAnalysis.Domain.HEALTH, 500, vanilla, "health mechanic");
        if (contains(t, "fire_resistance", "poison_resistance", "wither_resistance", "resistance", "resist"))
            return s(EffectAnalysis.Domain.RESISTANCE, 430, vanilla, "resistance mechanic");
        if (contains(t, "armor", "barrier", "shield", "block", "dodge", "knockback"))
            return s(EffectAnalysis.Domain.DEFENSE, 560, vanilla, "defensive mechanic");
        if (contains(t, "attack_speed", "swing_speed", "cooldown"))
            return s(EffectAnalysis.Domain.ATTACK_SPEED, 480, vanilla, "attack-speed mechanic");
        if (contains(t, "speed", "swiftness", "movement", "mobility", "haste", "agility", "jump", "leap", "reach"))
            return s(EffectAnalysis.Domain.MOBILITY, 360, vanilla, "mobility mechanic");
        if (contains(t, "mana", "energy", "resource", "stamina", "spirit", "aura"))
            return s(EffectAnalysis.Domain.RESOURCE, 330, vanilla, "resource mechanic");
        if (contains(t, "blind", "silence", "slow", "slowness", "root", "stun", "freeze", "control"))
            return s(EffectAnalysis.Domain.CONTROL, 440, vanilla, "control mechanic");
        if (contains(t, "night_vision", "vision", "sight", "darkness", "glow", "glowing", "visibility", "invisibility"))
            return s(EffectAnalysis.Domain.VISION, 300, vanilla, "vision/visibility mechanic");
        if (contains(t, "loot", "drop", "luck", "fortune", "find", "experience", "xp", "profession", "rarity", "quantity"))
            return s(EffectAnalysis.Domain.ECONOMY, 300, vanilla, "economy mechanic");
        if (contains(t, "poison", "venom", "wither", "weakness", "slowness", "mining_fatigue", "hunger", "nausea", "bad_omen", "darkness"))
            return s(EffectAnalysis.Domain.CONTROL, 430, vanilla, "negative/control mechanic");
        if (contains(t, "water_breathing", "breathing", "falling", "conduit", "utility"))
            return s(EffectAnalysis.Domain.UTILITY, 280, vanilla, "utility mechanic");
        return new Semantic(EffectAnalysis.Domain.UNKNOWN, 180, vanilla ? 0.72 : 0.50, "unclassified mechanic");
    }

    private static Semantic s(EffectAnalysis.Domain domain, double power, boolean vanilla, String reason) {
        return new Semantic(domain, power, vanilla ? 0.96 : 0.82, reason);
    }

    private static double attributeReference(EffectAnalysis.Domain domain, AttributeModifier.Operation operation) {
        return switch (domain) {
            case HEALTH -> 4.0;
            case DAMAGE, DEFENSE, RESISTANCE -> 2.0;
            case MOBILITY -> 0.02;
            case ATTACK_SPEED -> 0.10;
            case CRIT -> 0.05;
            case RESOURCE, REGENERATION -> 5.0;
            default -> operation == AttributeModifier.Operation.ADDITION ? 1.0 : 0.10;
        };
    }

    private static double mnsReference(EffectAnalysis.Domain domain, String description, double amount) {
        if (description.contains("%")) return 5.0;
        return switch (domain) {
            case HEALTH -> 10.0;
            case DAMAGE, DEFENSE, RESISTANCE, MOBILITY, ATTACK_SPEED, CRIT -> 5.0;
            case REGENERATION -> 2.0;
            default -> Math.max(1.0, amount);
        };
    }

    private static EffectAnalysis.Decision decide(double power, double confidence) {
        if (confidence < LIMITED_CONFIDENCE) return EffectAnalysis.Decision.EXCLUDED;
        if (confidence < MIN_CONFIDENCE || power > MAX_AUTO_POWER) return EffectAnalysis.Decision.LIMITED;
        return EffectAnalysis.Decision.APPROVED;
    }

    private static EffectAnalysis result(EffectAnalysis.Decision decision, EffectAnalysis.Domain domain,
                                         double power, double confidence, String reason) {
        return new EffectAnalysis(decision, domain, power, confidence,
                (int) Math.round(clamp(power, 20.0, 900.0)), reason);
    }

    private static EffectAnalysis excluded(String reason) {
        return new EffectAnalysis(EffectAnalysis.Decision.EXCLUDED, EffectAnalysis.Domain.UNKNOWN,
                1000.0, 0.0, 950, reason);
    }

    private static String semanticText(ResourceLocation id, String extra) {
        return id.getNamespace() + " " + id.getPath() + " " + (extra == null ? "" : extra);
    }

    private static boolean isDangerous(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        return contains(t, "instant_damage", "instantdamage", "detonation", "detonate", "explosion", "explode",
                "self_damage", "selfdamage", "health_drain", "healthdrain", "hemorrhage", "bleed", "wound", "agony");
    }

    private static boolean contains(String text, String... terms) {
        for (String term : terms) if (text.contains(term)) return true;
        return false;
    }

    private static double log2(double value) { return Math.log(value) / Math.log(2.0); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private record Semantic(EffectAnalysis.Domain domain, double basePower, double confidence, String reason) {}
}
