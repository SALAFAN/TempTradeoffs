package com.warg.temptradeoffs.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

public record Tradeoff(
        String id,
        String titleKey,
        Rarity rarity,
        List<ModifierSpec> positive,
        List<ModifierSpec> negative
) {
    public enum Rarity {
        COMMON("screen.temptradeoffs.rarity.common", 0xFFE0E0E0, 1000, 1000),
        UNCOMMON("screen.temptradeoffs.rarity.uncommon", 0xFF55FF55, 1250, 1250),
        RARE("screen.temptradeoffs.rarity.rare", 0xFF5555FF, 1500, 1500),
        EPIC("screen.temptradeoffs.rarity.epic", 0xFFAA00AA, 1750, 1750),
        LEGENDARY("screen.temptradeoffs.rarity.legendary", 0xFFFFAA00, 2000, 2000),
        CURSED("screen.temptradeoffs.rarity.cursed", 0xFFAA0000, 4000, Integer.MAX_VALUE),
        BLESSED("screen.temptradeoffs.rarity.blessed", 0xFF55FFFF, 2500, 750);

        private final String translationKey;
        private final int color;
        private final int positiveBudget;
        private final int negativeBudget;

        Rarity(String translationKey, int color, int positiveBudget, int negativeBudget) {
            this.translationKey = translationKey;
            this.color = color;
            this.positiveBudget = positiveBudget;
            this.negativeBudget = negativeBudget;
        }

        public String translationKey() { return translationKey; }
        public int color() { return color; }
        public int positiveBudget() { return positiveBudget; }
        public int negativeBudget() { return negativeBudget; }
    }
    public enum ModifierType {
        MOB_EFFECT,
        ATTRIBUTE,
        MNS_STAT
    }

    public record ModifierSpec(
            ModifierType type,
            ResourceLocation id,
            int amplifier,
            double amount,
            AttributeModifier.Operation operation,
            int weight,
            String mnsModType
    ) {
        public ModifierSpec(ModifierType type, ResourceLocation id, int amplifier, double amount,
                            AttributeModifier.Operation operation, int weight) {
            this(type, id, amplifier, amount, operation, weight, "FLAT");
        }

        /** Stable per-variant key used by the config UI. It deliberately does not
         * include the user-editable amount, so changing a value cannot orphan its
         * saved weight/enabled state. The default weight disambiguates variants
         * that share the same id/operation (for example attribute value variants). */
        public String stableKey() {
            return type.name().toLowerCase(java.util.Locale.ROOT) + ":" + id
                    + ":amp=" + amplifier + ":defaultWeight=" + weight
                    + ":op=" + operation.name() + ":mns=" + mnsModType;
        }

        public String valueKey() {
            return type.name().toLowerCase(java.util.Locale.ROOT) + ":" + id
                    + ":amp=" + amplifier + ":defaultWeight=" + weight
                    + ":op=" + operation.name() + ":mns=" + mnsModType;
        }

        public String familyKey() {
            return type.name().toLowerCase(java.util.Locale.ROOT) + ":" + id;
        }

        public String displayId() {
            return id.toString();
        }
    }
}
