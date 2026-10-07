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
        ATTRIBUTE
    }

    public record ModifierSpec(
            ModifierType type,
            ResourceLocation id,
            int amplifier,
            double amount,
            AttributeModifier.Operation operation,
            int weight
    ) {
        public String stableKey() {
            return type.name().toLowerCase() + ":" + id;
        }

        public String displayId() {
            return id.toString();
        }
    }
}
