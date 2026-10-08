package com.warg.temptradeoffs.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

public record Tradeoff(
        String id,
        String titleKey,
        List<ModifierSpec> positive,
        List<ModifierSpec> negative
) {
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
