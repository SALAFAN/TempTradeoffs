package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

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
        for (int i = 0; i < choices.size(); i++) drawChoice(g, choices.get(i), start + i * (w + gap), panelTop, w);
        super.render(g, mouseX, mouseY, partial);
    }

    private void drawChoice(GuiGraphics g, TTPackets.ChoiceView c, int x, int y, int w) {
        int h = 355;
        g.fill(x, y, x + w, y + h, 0xF0171717);
        g.fill(x, y, x + w, y + 2, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable(c.titleKey()), x + w / 2, y + 12, 0xFFFFFF);
        int yy = y + 34;
        g.drawString(font, Component.translatable("screen.temptradeoffs.positive"), x + 10, yy, 0x55FF55); yy += 16;
        for (var m : c.positive()) { drawModifier(g, m, x + 10, yy, 0x66FF66); yy += 18; }
        yy += 4;
        g.drawString(font, Component.translatable("screen.temptradeoffs.negative"), x + 10, yy, 0xFF5555); yy += 16;
        for (var m : c.negative()) { drawModifier(g, m, x + 10, yy, 0xFF7777); yy += 18; }
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.permanent"), x + w / 2, y + h - 18, 0xAAAAAA);
    }

    private void drawModifier(GuiGraphics g, TTPackets.ModifierView m, int x, int y, int color) {
        String name = getModifierName(m);
        String value = getModifierValue(m);
        if (name.length() > 25) name = name.substring(0, 24) + "…";
        g.drawString(font, name, x, y, color);
        if (!value.isEmpty()) g.drawString(font, value, x + 177 - font.width(value), y, color);
    }

    private String getModifierName(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            return effect == null ? m.id().toString() : Component.translatable(effect.getDescriptionId()).getString() + (m.amplifier() > 0 ? " " + (m.amplifier() + 1) : "");
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(m.id());
        return attribute == null ? m.id().toString() : Component.translatable(attribute.getDescriptionId()).getString();
    }

    private String getModifierValue(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) return "";
        return String.format("%+.1f%%", m.amount() * 100.0);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
