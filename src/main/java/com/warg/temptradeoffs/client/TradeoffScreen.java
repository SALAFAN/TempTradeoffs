package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import java.util.ArrayList;

import java.util.List;

public class TradeoffScreen extends Screen {
    private final List<TTPackets.ChoiceView> choices;
    private Button[] buttons;
    private int panelTop;

    public TradeoffScreen(List<TTPackets.ChoiceView> choices) { super(Component.translatable("screen.temptradeoffs.title")); this.choices = choices; }

    @Override protected void init() {
        buttons = new Button[choices.size()];
        int w = 205, gap = 10, total = w * choices.size() + gap * (choices.size() - 1), start = (width - total) / 2;
        panelTop = Math.max(32, (height - 410) / 2);
        for (int i = 0; i < choices.size(); i++) {
            int x = start + i * (w + gap); final int idx = i;
            buttons[i] = addRenderableWidget(Button.builder(Component.translatable("screen.temptradeoffs.choose"), b -> {
                TTPackets.CHANNEL.sendToServer(new TTPackets.SelectChoicePacket(idx));
                ClientScreenOpener.closeChoice();
            }).bounds(x, panelTop + 370, w, 22).build());
        }
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xC8000000);
        g.drawCenteredString(font, title, width / 2, Math.max(10, panelTop - 22), 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.subtitle"), width / 2, Math.max(24, panelTop - 6), 0xCCCCCC);
        int w = 205, gap = 10, total = w * choices.size() + gap * (choices.size() - 1), start = (width - total) / 2;
        TTPackets.ModifierView hovered = null;
        for (int i = 0; i < choices.size(); i++) {
            int x = start + i * (w + gap);
            hovered = drawChoice(g, choices.get(i), x, panelTop, w, mouseX, mouseY, hovered);
        }
        super.render(g, mouseX, mouseY, partial);
        if (hovered != null) g.renderTooltip(font, ModifierTooltip.lines(hovered), mouseX, mouseY);
    }

    private TTPackets.ModifierView drawChoice(GuiGraphics g, TTPackets.ChoiceView c, int x, int y, int w, int mouseX, int mouseY, TTPackets.ModifierView hovered) {
        int h = 355;
        g.fill(x, y, x + w, y + h, 0xF0171717);
        g.fill(x, y, x + w, y + 3, c.rarity().color());
        g.drawCenteredString(font, Component.translatable(c.rarity().translationKey()), x + w / 2, y + 8, c.rarity().color());
        g.drawCenteredString(font, Component.translatable(c.titleKey()), x + w / 2, y + 21, 0xFFFFFF);
        int yy = y + 40;
        g.drawString(font, Component.translatable("screen.temptradeoffs.positive"), x + 10, yy, 0x55FF55); yy += 16;
        if (c.positive().isEmpty()) {
            g.drawString(font, Component.translatable("screen.temptradeoffs.no_effects"), x + 10, yy, 0xFFAA55);
            yy += 18;
        }
        for (var m : c.positive()) {
            if (mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= yy - 2 && mouseY <= yy + 14) hovered = m;
            drawModifier(g, m, x + 10, yy, 0x66FF66); yy += 18;
        }
        yy += 4;
        g.drawString(font, Component.translatable("screen.temptradeoffs.negative"), x + 10, yy, 0xFF5555); yy += 16;
        if (c.negative().isEmpty()) {
            g.drawString(font, Component.translatable("screen.temptradeoffs.no_effects"), x + 10, yy, 0xFFAA55);
            yy += 18;
        }
        for (var m : c.negative()) {
            if (mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= yy - 2 && mouseY <= yy + 14) hovered = m;
            drawModifier(g, m, x + 10, yy, 0xFF7777); yy += 18;
        }
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.permanent"), x + w / 2, y + h - 18, 0xAAAAAA);
        return hovered;
    }

    private void drawModifier(GuiGraphics g, TTPackets.ModifierView m, int x, int y, int color) {
        String name = getModifierName(m);
        String value = getModifierValue(m);
        if (name.length() > 25) name = name.substring(0, 24) + "…";
        g.drawString(font, name, x, y, color);
        if (!value.isEmpty()) g.drawString(font, value, x + 177 - font.width(value), y, color);
    }

    private String getModifierName(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MNS_STAT) {
            return m.displayName().isEmpty() ? m.id().toString() : m.displayName();
        }
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            return effect == null ? m.id().toString() : Component.translatable(effect.getDescriptionId()).getString() + (m.amplifier() > 0 ? " " + (m.amplifier() + 1) : "");
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(m.id());
        return attribute == null ? m.id().toString() : Component.translatable(attribute.getDescriptionId()).getString();
    }

    private String getModifierValue(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MNS_STAT) return m.displayValue();
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) return "";
        return String.format("%+.1f%%", m.amount() * 100.0);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
