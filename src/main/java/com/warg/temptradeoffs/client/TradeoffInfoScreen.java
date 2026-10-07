package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

public class TradeoffInfoScreen extends Screen {
    private final TTPackets.ChoiceView current;
    private final List<Row> rows = new ArrayList<>();

    private record Row(int x, int y, int w, int h, TTPackets.ModifierView modifier, boolean positive) {}

    public TradeoffInfoScreen(TTPackets.ChoiceView current) {
        super(Component.translatable("screen.temptradeoffs.current_title"));
        this.current = current;
    }

    @Override protected void init() {
        rows.clear();
        int x = Math.max(20, (width - 560) / 2);
        int y = 65;
        for (var m : current.positive()) { rows.add(new Row(x, y, 560, 22, m, true)); y += 24; }
        y += 12;
        for (var m : current.negative()) { rows.add(new Row(x, y, 560, 22, m, false)); y += 24; }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable(current.titleKey()), width / 2, 40, 0xCCCCCC);
        for (Row row : rows) {
            int c = row.positive ? 0x55FF55 : 0xFF6666;
            g.drawString(font, getName(row.modifier), row.x, row.y, c);
            String value = getValue(row.modifier);
            if (!value.isEmpty()) g.drawString(font, value, row.x + row.w - font.width(value), row.y, c);
            if (mouseX >= row.x && mouseX <= row.x + row.w && mouseY >= row.y && mouseY <= row.y + row.h) {
                g.renderTooltip(font, tooltip(row.modifier), mouseX, mouseY);
            }
        }
        super.render(g, mouseX, mouseY, partial);
    }

    private String getName(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect e = BuiltInRegistries.MOB_EFFECT.get(m.id());
            return e == null ? m.id().toString() : Component.translatable(e.getDescriptionId()).getString() + (m.amplifier() > 0 ? " " + (m.amplifier()+1) : "");
        }
        Attribute a = BuiltInRegistries.ATTRIBUTE.get(m.id());
        return a == null ? m.id().toString() : Component.translatable(a.getDescriptionId()).getString();
    }

    private String getValue(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) return Component.translatable("screen.temptradeoffs.permanent").getString();
        double pct = m.amount() * 100.0;
        return String.format("%+.1f%%", pct);
    }

    private List<Component> tooltip(TTPackets.ModifierView m) {
        List<Component> out = new ArrayList<>();
        out.add(Component.literal(getName(m)));
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            out.add(Component.translatable("screen.temptradeoffs.tooltip.effect"));
            out.add(Component.translatable("screen.temptradeoffs.permanent"));
        } else {
            out.add(Component.translatable("screen.temptradeoffs.tooltip.attribute"));
            out.add(Component.literal(getValue(m)));
        }
        out.add(Component.literal(m.id().toString()));
        return out;
    }

    @Override public boolean isPauseScreen() { return false; }
}
