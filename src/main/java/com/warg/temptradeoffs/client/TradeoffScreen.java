package com.warg.temptradeoffs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import com.mojang.math.Axis;

import java.util.List;

/** Minecraft-styled mandatory card selection screen. */
public class TradeoffScreen extends Screen {
    private static final int CARD_W = 205;
    private static final int CARD_H = 355;
    private static final int GAP = 10;
    private static final float EFFECT_TEXT_SCALE = 0.86F;

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
            // The widget keeps the click/keyboard handling, while its vanilla
            // renderer is hidden and replaced by the pixel button drawn below.
            buttons[i].setAlpha(0.0F);
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

        // Draw the actual Minecraft-like buttons before the transparent widgets.
        for (int i = 0; i < buttons.length; i++) drawChoiceButton(g, buttons[i], choices.get(i).rarity().color(), mouseX, mouseY);

        super.render(g, mouseX, mouseY, partial);
        if (hovered != null) g.renderTooltip(font, ModifierTooltip.lines(hovered), mouseX, mouseY);
    }

    private TTPackets.ModifierView drawChoice(GuiGraphics g, TTPackets.ChoiceView c, int x, int y,
                                               int w, int mouseX, int mouseY, TTPackets.ModifierView hovered) {
        drawPanel(g, x, y, w, CARD_H, 0xFF2B211A);
        g.fill(x + 4, y + 4, x + w - 4, y + CARD_H - 4, 0xFF111111);
        g.fill(x + 6, y + 6, x + w - 6, y + 31, 0xFF252525);
        g.fill(x + 6, y + 30, x + w - 6, y + 32, 0xFF0B0B0B);

        int rarity = c.rarity().color();
        g.fill(x + 6, y + 6, x + w - 6, y + 9, rarity);
        g.drawCenteredString(font, Component.translatable(c.titleKey()), x + w / 2, y + 12, rarity);
        drawRaritySticker(g, c.rarity(), x, y, w);

        int yy = y + 43;
        yy = drawSection(g, c.positive(), x, yy, w, true);
        hovered = drawModifiers(g, c.positive(), x + 10, yy, w - 20, true, mouseX, mouseY, hovered);
        yy += c.positive().isEmpty() ? 20 : c.positive().size() * 18;
        yy += 8;
        yy = drawSection(g, c.negative(), x, yy, w, false);
        hovered = drawModifiers(g, c.negative(), x + 10, yy, w - 20, false, mouseX, mouseY, hovered);

        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.permanent"),
                x + w / 2, y + CARD_H - 17, 0xFF8A8A8A);
        return hovered;
    }

    private void drawRaritySticker(GuiGraphics g, com.warg.temptradeoffs.common.Tradeoff.Rarity rarity, int x, int y, int w) {
        PoseStack pose = g.pose();
        pose.pushPose();

        // Compact corner sticker. The width is based on the longest rarity name,
        // so the sticker is only as wide as necessary to fit "Благословленная".
        final float textScale = 0.78F;
        final int horizontalPadding = 12;
        Component text = Component.translatable(rarity.translationKey());
        Component longest = Component.translatable(com.warg.temptradeoffs.common.Tradeoff.Rarity.BLESSED.translationKey());
        int stickerWidth = Math.max(74,
                (int) Math.ceil(font.width(longest) / textScale) + horizontalPadding);
        int stickerHeight = 20;

        // Positive rotation in GUI coordinates places the left edge higher and the right edge lower.
        float cx = x + w - stickerWidth * 0.5F - 5.0F;
        float cy = y + 21.0F;
        pose.translate(cx, cy, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(12.0F));

        int half = stickerWidth / 2;
        int halfInner = half - 2;
        int color = rarity.color();
        int dark = darken(color, 0.48F);
        int light = lighten(color, 0.24F);

        // Pixel/beveled sticker border and face.
        g.fill(-half, -stickerHeight / 2, half, stickerHeight / 2, 0xFF101010);
        g.fill(-half + 1, -stickerHeight / 2 + 1, half - 1, stickerHeight / 2 - 1, dark);
        g.fill(-halfInner, -stickerHeight / 2 + 3, halfInner, stickerHeight / 2 - 3, color);
        g.fill(-halfInner, -stickerHeight / 2 + 3, halfInner, -stickerHeight / 2 + 5, light);
        g.fill(-halfInner, stickerHeight / 2 - 5, halfInner, stickerHeight / 2 - 3, darken(color, 0.35F));

        pose.pushPose();
        pose.scale(textScale, textScale, 1.0F);
        g.drawCenteredString(font, text, 0, -4, 0xFFFFFFFF);
        pose.popPose();
        pose.popPose();
    }

    private int drawSection(GuiGraphics g, List<TTPackets.ModifierView> list, int x, int y, int w,
                            boolean positive) {
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
        int maxNameWidth = Math.max(20, w - 8 - (value.isEmpty() ? 0 : font.width(value) + 5));
        name = trimScaled(name, maxNameWidth);

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale((float) EFFECT_TEXT_SCALE, (float) EFFECT_TEXT_SCALE, 1.0F);
        g.drawString(font, name, 0, 0, color);
        if (!value.isEmpty()) {
            float right = w / (float) EFFECT_TEXT_SCALE;
            g.drawString(font, value, (int) (right - font.width(value)), 0, color);
        }
        pose.popPose();
    }

    private String trimScaled(String value, int maxWidth) {
        if (font.width(value) * EFFECT_TEXT_SCALE <= maxWidth) return value;
        String suffix = "…";
        while (!value.isEmpty() && font.width(value + suffix) * EFFECT_TEXT_SCALE > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private void drawChoiceButton(GuiGraphics g, Button button, int rarityColor, int mouseX, int mouseY) {
        int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        boolean pressed = hovered && minecraft != null && minecraft.mouseHandler.isLeftPressed();
        int border = darken(rarityColor, hovered ? 0.58F : 0.72F);
        int fill = hovered ? lighten(rarityColor, 0.10F) : darken(rarityColor, 0.45F);
        if (pressed) fill = darken(rarityColor, 0.60F);

        g.fill(x, y, x + w, y + h, 0xFF101010);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, border);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, fill);
        g.fill(x + 3, y + 3, x + w - 3, y + 4, lighten(fill, 0.28F));
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, darken(fill, 0.35F));

        int textColor = hovered ? 0xFFFFFFFF : 0xFFE8E8E8;
        g.drawCenteredString(font, button.getMessage(), x + w / 2, y + 6, textColor);
    }

    private int darken(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >>> 16) & 0xFF) * factor);
        int gr = (int) (((color >>> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private int lighten(int color, float amount) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int gr = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        r = (int) (r + (255 - r) * amount);
        gr = (int) (gr + (255 - gr) * amount);
        b = (int) (b + (255 - b) * amount);
        return (a << 24) | (r << 16) | (gr << 8) | b;
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
