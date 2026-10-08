package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;

/** Minecraft-styled mandatory card selection screen. */
public class TradeoffScreen extends Screen {
    private static final int CARD_W = 205;
    private static final int CARD_H = 355;
    private static final int GAP = 10;
    private static final int FRAME = 3;

    private final List<TTPackets.ChoiceView> choices;
    private Button[] buttons;
    private int panelTop;

    public TradeoffScreen(List<TTPackets.ChoiceView> choices) {
        super(Component.translatable("screen.temptradeoffs.title"));
        this.choices = choices;
    }

    @Override
    protected void init() {
        buttons = new Button[choices.size()];
        int total = CARD_W * choices.size() + GAP * (choices.size() - 1);
        int start = (width - total) / 2;
        panelTop = Math.max(34, (height - 410) / 2);
        for (int i = 0; i < choices.size(); i++) {
            int x = start + i * (CARD_W + GAP);
            final int idx = i;
            buttons[i] = addRenderableWidget(Button.builder(
                    Component.translatable("screen.temptradeoffs.choose"),
                    b -> {
                        TTPackets.CHANNEL.sendToServer(new TTPackets.SelectChoicePacket(idx));
                        ClientScreenOpener.closeChoice();
                    }).bounds(x + 10, panelTop + 368, CARD_W - 20, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.fill(0, 0, width, height, 0x33000000);

        g.drawCenteredString(font, title, width / 2, Math.max(10, panelTop - 24), 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.subtitle"),
                width / 2, Math.max(24, panelTop - 8), 0xFFBFBFBF);

        int total = CARD_W * choices.size() + GAP * (choices.size() - 1);
        int start = (width - total) / 2;
        TTPackets.ModifierView hovered = null;
        for (int i = 0; i < choices.size(); i++) {
            int x = start + i * (CARD_W + GAP);
            hovered = drawChoice(g, choices.get(i), x, panelTop, CARD_W, mouseX, mouseY, hovered);
        }

        super.render(g, mouseX, mouseY, partial);
        if (hovered != null) g.renderTooltip(font, ModifierTooltip.lines(hovered), mouseX, mouseY);
    }

    private TTPackets.ModifierView drawChoice(GuiGraphics g, TTPackets.ChoiceView c, int x, int y,
                                               int w, int mouseX, int mouseY, TTPackets.ModifierView hovered) {
        // Pixel/bevel frame inspired by vanilla inventory/container screens.
        drawPanel(g, x, y, w, CARD_H, 0xFF2B211A);
        g.fill(x + 4, y + 4, x + w - 4, y + CARD_H - 4, 0xFF111111);
        g.fill(x + 6, y + 6, x + w - 6, y + 31, 0xFF252525);
        g.fill(x + 6, y + 30, x + w - 6, y + 32, 0xFF0B0B0B);

        int rarity = c.rarity().color();
        g.fill(x + 6, y + 6, x + w - 6, y + 9, rarity);
        g.drawCenteredString(font, Component.translatable(c.rarity().translationKey()), x + w / 2, y + 12, rarity);
        g.drawCenteredString(font, Component.translatable(c.titleKey()), x + w / 2, y + 24, 0xFFFFFFFF);

        int yy = y + 43;
        yy = drawSection(g, c.positive(), x, yy, w, true, mouseX, mouseY, hovered);
        // drawSection cannot return the hovered object through an int, so the
        // actual hover pass is repeated below while drawing the rows.
        hovered = drawModifiers(g, c.positive(), x + 10, yy, w - 20, true, mouseX, mouseY, hovered);
        yy += c.positive().isEmpty() ? 20 : c.positive().size() * 18;
        yy += 8;
        yy = drawSection(g, c.negative(), x, yy, w, false, mouseX, mouseY, hovered);
        hovered = drawModifiers(g, c.negative(), x + 10, yy, w - 20, false, mouseX, mouseY, hovered);

        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.permanent"),
                x + w / 2, y + CARD_H - 17, 0xFF8A8A8A);
        return hovered;
    }

    private int drawSection(GuiGraphics g, List<TTPackets.ModifierView> list, int x, int y, int w,
                            boolean positive, int mouseX, int mouseY, TTPackets.ModifierView hovered) {
        int color = positive ? 0xFF55FF55 : 0xFFFF5555;
        Component label = Component.translatable(positive
                ? "screen.temptradeoffs.positive" : "screen.temptradeoffs.negative");
        g.fill(x + 8, y + 6, x + 34, y + 8, color);
        g.drawString(font, label, x + 39, y + 1, color);
        return y + 18;
    }

    private TTPackets.ModifierView drawModifiers(GuiGraphics g, List<TTPackets.ModifierView> list,
                                                   int x, int y, int w, boolean positive,
                                                   int mouseX, int mouseY, TTPackets.ModifierView hovered) {
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("screen.temptradeoffs.no_effects"), x, y, 0xFFFFAA55);
            return hovered;
        }
        int yy = y;
        for (TTPackets.ModifierView m : list) {
            if (mouseX >= x - 3 && mouseX <= x + w + 3 && mouseY >= yy - 2 && mouseY <= yy + 14) {
                g.fill(x - 3, yy - 3, x + w + 3, yy + 15, 0x22101010);
                hovered = m;
            }
            drawModifier(g, m, x, yy, w, positive ? 0xFF66FF66 : 0xFFFF7777);
            yy += 18;
        }
        return hovered;
    }

    private void drawModifier(GuiGraphics g, TTPackets.ModifierView m, int x, int y, int w, int color) {
        String name = getModifierName(m);
        String value = getModifierValue(m);
        if (name.length() > 25) name = name.substring(0, 24) + "…";
        g.drawString(font, name, x, y, color);
        if (!value.isEmpty()) g.drawString(font, value, x + w - font.width(value), y, color);
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h, int base) {
        g.fill(x, y, x + w, y + h, base);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF555555);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xFF171717);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF777777);
        g.fill(x + 3, y + 3, x + 4, y + h - 3, 0xFF777777);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0xFF0B0B0B);
        g.fill(x + w - 4, y + 3, x + w - 3, y + h - 3, 0xFF0B0B0B);
    }

    private String getModifierName(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MNS_STAT) {
            return m.displayName().isEmpty() ? m.id().toString() : m.displayName();
        }
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            return effect == null ? m.id().toString() : Component.translatable(effect.getDescriptionId()).getString()
                    + (m.amplifier() > 0 ? " " + (m.amplifier() + 1) : "");
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
