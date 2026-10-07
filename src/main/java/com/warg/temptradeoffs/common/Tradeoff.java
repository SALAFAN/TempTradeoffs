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

        public String stableKey() {
            return type.name().toLowerCase(java.util.Locale.ROOT) + ":" + id + ":amp=" + amplifier + ":amount=" + String.format(java.util.Locale.ROOT, "%.4f", amount) + ":op=" + operation.name() + ":mns=" + mnsModType;
        }

        public String displayId() {
            return id.toString();
        }
    }
}
