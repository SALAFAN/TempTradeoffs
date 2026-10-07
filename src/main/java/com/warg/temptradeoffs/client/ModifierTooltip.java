package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shared names, values and hover descriptions for every modifier shown by TempTradeoffs. */
public final class ModifierTooltip {
    public static String name(TTPackets.ModifierView m) {
        if (m.type() == Tradeoff.ModifierType.MNS_STAT)
            return m.displayName().isEmpty() ? m.id().toString() : m.displayName();
        if (m.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            if (effect == null) return m.id().toString();
            String n = Component.translatable(effect.getDescriptionId()).getString();
            return m.amplifier() > 0 ? n + " " + (m.amplifier() + 1) : n;
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(m.id());
        return attribute == null ? m.id().toString() : Component.translatable(attribute.getDescriptionId()).getString();
    }

    public static String value(TTPackets.ModifierView m) {
        if (m.type() == Tradeoff.ModifierType.MNS_STAT) return m.displayValue();
        if (m.type() == Tradeoff.ModifierType.MOB_EFFECT)
            return Component.translatable("screen.temptradeoffs.permanent_short").getString();
        return String.format(Locale.ROOT, "%+.1f%%", m.amount() * 100.0);
    }

    public static List<FormattedCharSequence> lines(TTPackets.ModifierView m) {
        List<FormattedCharSequence> out = new ArrayList<>();
        out.add(Component.literal(name(m)).getVisualOrderText());
        String description = description(m);
        if (!description.isEmpty()) out.add(Component.literal(description).getVisualOrderText());
        String value = value(m);
        if (!value.isEmpty()) out.add(Component.literal(value).getVisualOrderText());
        out.add(Component.literal(m.id().toString()).getVisualOrderText());
        return out;
    }

    public static String description(TTPackets.ModifierView m) {
        if (!m.description().isEmpty()) return m.description();
        if (m.type() == Tradeoff.ModifierType.MNS_STAT) return Component.translatable("screen.temptradeoffs.tooltip.mns").getString();

        String path = m.id().getPath().toLowerCase(Locale.ROOT);
        String key = "modifier_description.temptradeoffs." + m.type().name().toLowerCase(Locale.ROOT) + "." + path;
        if (I18n.exists(key)) return Component.translatable(key).getString();

        if (m.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            if (effect != null) {
                String vanillaKey = effect.getDescriptionId() + ".description";
                if (I18n.exists(vanillaKey)) return Component.translatable(vanillaKey).getString();
            }
            return Component.translatable("screen.temptradeoffs.tooltip.effect_generic").getString();
        }
        return Component.translatable("screen.temptradeoffs.tooltip.attribute_generic").getString();
    }

    private ModifierTooltip() {}
}
