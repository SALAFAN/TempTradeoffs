package com.warg.temptradeoffs.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public record Tradeoff(String id, String titleKey, List<EffectSpec> positive, List<EffectSpec> negative) {
    public record EffectSpec(ResourceLocation effect, int amplifier) {
        public MobEffectInstance create(int duration) {
            MobEffect e = BuiltInRegistries.MOB_EFFECT.get(effect);
            if (e == null) throw new IllegalArgumentException("Unknown effect: " + effect);
            return new MobEffectInstance(e, duration, amplifier, false, true, true);
        }
    }

    public static EffectSpec e(String id, int amplifier) { return new EffectSpec(new ResourceLocation(id), amplifier); }
    public static Tradeoff of(String id, String titleKey, EffectSpec p, EffectSpec n) {
        return new Tradeoff(id, titleKey, List.of(p), List.of(n));
    }
}
