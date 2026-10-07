package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;

public class TradeoffScreen extends Screen {
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

        int w = 205;
        int gap = 10;
        int total = w * choices.size() + gap * (choices.size() - 1);
        int start = (width - total) / 2;

        panelTop = Math.max(42, (height - 350) / 2);

        for (int i = 0; i < choices.size(); i++) {
            int x = start + i * (w + gap);
            final int idx = i;

            buttons[i] = addRenderableWidget(
                    Button.builder(
                                    Component.translatable("screen.temptradeoffs.choose"),
                                    b -> {
                                        com.warg.temptradeoffs.network.TTPackets.CHANNEL.sendToServer(
                                                new TTPackets.SelectChoicePacket(idx)
                                        );
                                        ClientScreenOpener.closeChoice();
                                    }
                            )
                            .bounds(x, panelTop + 315, w, 22)
                            .build()
            );
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        // Darken whatever GUI is currently underneath the choice.
        g.fill(0, 0, width, height, 0xB8000000);

        g.drawCenteredString(
                font,
                title,
                width / 2,
                Math.max(12, panelTop - 28),
                0xFFFFFF
        );

        g.drawCenteredString(
                font,
                Component.translatable("screen.temptradeoffs.subtitle"),
                width / 2,
                Math.max(28, panelTop - 10),
                0xCCCCCC
        );

        int w = 205;
        int gap = 10;
        int total = w * choices.size() + gap * (choices.size() - 1);
        int start = (width - total) / 2;

        for (int i = 0; i < choices.size(); i++) {
            drawChoice(g, choices.get(i), start + i * (w + gap), panelTop, w);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    private void drawChoice(GuiGraphics g, TTPackets.ChoiceView c, int x, int y, int w) {
        int h = 300;

        g.fill(x, y, x + w, y + h, 0xF0171717);
        g.fill(x, y, x + w, y + 2, 0xFFFFFFFF);

        Component title = Component.translatable(c.titleKey());
        g.drawCenteredString(font, title, x + w / 2, y + 12, 0xFFFFFF);

        int yy = y + 36;

        g.drawString(font, Component.translatable("screen.temptradeoffs.positive"), x + 10, yy, 0x55FF55);
        yy += 16;

        for (var m : c.positive()) {
            drawModifier(g, m, x + 14, yy, 0x66FF66);
            yy += 20;
        }

        yy += 5;

        g.drawString(font, Component.translatable("screen.temptradeoffs.negative"), x + 10, yy, 0xFF5555);
        yy += 16;

        for (var m : c.negative()) {
            drawModifier(g, m, x + 14, yy, 0xFF7777);
            yy += 20;
        }

        String min = (c.durationTicks() / 1200) + " min";
        g.drawCenteredString(
                font,
                Component.translatable("screen.temptradeoffs.expires", min),
                x + w / 2,
                y + h - 18,
                0xAAAAAA
        );
    }

    private void drawModifier(
            GuiGraphics g,
            TTPackets.ModifierView m,
            int x,
            int y,
            int color
    ) {
        String name = getModifierName(m);
        String value = getModifierValue(m);

        g.drawString(font, name, x, y, color);

        if (!value.isEmpty()) {
            int valueWidth = font.width(value);
            g.drawString(font, value, x + 177 - valueWidth, y, color);
        }
    }

    private String getModifierName(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            if (effect == null) {
                return m.id().toString();
            }

            return Component.translatable(effect.getDescriptionId()).getString()
                    + (m.amplifier() > 0 ? " " + (m.amplifier() + 1) : "");
        }

        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(m.id());
        if (attribute == null) {
            return m.id().toString();
        }

        return Component.translatable(attribute.getDescriptionId()).getString();
    }

    private String getModifierValue(TTPackets.ModifierView m) {
        if (m.type() == com.warg.temptradeoffs.common.Tradeoff.ModifierType.MOB_EFFECT) {
            return "";
        }

        double percent = m.amount() * 100.0;
        if (Math.abs(percent - Math.rint(percent)) < 0.001) {
            return String.format("%+.0f%%", percent);
        }

        return String.format("%+.1f%%", percent);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
