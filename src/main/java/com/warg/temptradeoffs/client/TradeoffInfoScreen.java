package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Просмотр уже выбранной карты. Здесь нельзя выбрать новый вариант — экран
 * предназначен только для просмотра текущих бонусов и штрафов.
 */
public class TradeoffInfoScreen extends Screen {
    private static final int PANEL_W = 610;
    private static final int PANEL_TOP = 28;
    private static final int PANEL_BOTTOM = 32;
    private static final int ROW_H = 28;

    private final TTPackets.ChoiceView current;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int contentTop;
    private int contentBottom;
    private int maxScroll;

    private record Row(TTPackets.ModifierView modifier, boolean positive) {}

    public TradeoffInfoScreen(TTPackets.ChoiceView current) {
        super(Component.translatable("screen.temptradeoffs.current_title"));
        this.current = current;
    }

    @Override
    protected void init() {
        rows.clear();
        for (TTPackets.ModifierView m : current.positive()) rows.add(new Row(m, true));
        for (TTPackets.ModifierView m : current.negative()) rows.add(new Row(m, false));

        contentTop = PANEL_TOP + 76;
        contentBottom = height - 58;
        int visibleHeight = Math.max(0, contentBottom - contentTop);
        int contentHeight = rows.size() * ROW_H + 12;
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scroll = Math.min(scroll, maxScroll);

        int closeW = 110;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                b -> onClose()
        ).bounds(width / 2 - closeW / 2, height - 27, closeW, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);

        int left = (width - PANEL_W) / 2;
        int right = left + PANEL_W;
        int top = PANEL_TOP;
        int bottom = height - PANEL_BOTTOM;

        // Основная панель.
        g.fill(left, top, right, bottom, 0xF0101010);
        g.fill(left, top, right, top + 2, 0xFFE0E0E0);
        g.fill(left, bottom - 1, right, bottom, 0xFF555555);
        g.fill(left, top, left + 1, bottom, 0xFF555555);
        g.fill(right - 1, top, right, bottom, 0xFF555555);

        g.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable(current.titleKey()), width / 2, top + 30, 0xFFD58A3A);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.current_hint"), width / 2, top + 48, 0xAAAAAA);

        // Обрезаем область списка, чтобы длинные наборы эффектов не залезали
        // на заголовок и кнопку закрытия.
        g.enableScissor(left + 8, contentTop, right - 8, contentBottom);
        TTPackets.ModifierView hoveredModifier = null;

        int y = contentTop + 4 - scroll;
        int positiveCount = current.positive().size();
        if (positiveCount > 0) {
            drawSection(g, left + 14, y, true);
            y += 22;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i == positiveCount) {
                y += 8;
                drawSection(g, left + 14, y, false);
                y += 22;
            }
            if (mouseX >= left + 12 && mouseX <= right - 12 && mouseY >= y && mouseY <= y + ROW_H - 3) {
                hoveredModifier = row.modifier;
            }
            drawRow(g, row, left + 12, y, PANEL_W - 24, mouseX, mouseY);
            y += ROW_H;
        }
        g.disableScissor();
        if (hoveredModifier != null) {
            g.renderTooltip(font, tooltip(hoveredModifier), mouseX, mouseY);
        }

        if (maxScroll > 0) {
            int trackX = right - 8;
            int trackTop = contentTop;
            int trackBottom = contentBottom;
            int trackH = trackBottom - trackTop;
            int thumbH = Math.max(20, trackH * trackH / (trackH + maxScroll));
            int thumbY = trackTop + (trackH - thumbH) * scroll / maxScroll;
            g.fill(trackX, trackTop, trackX + 3, trackBottom, 0x55333333);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFFAAAAAA);
        }

        super.render(g, mouseX, mouseY, partial);
    }

    private void drawSection(GuiGraphics g, int x, int y, boolean positive) {
        Component text = Component.translatable(positive
                ? "screen.temptradeoffs.positive"
                : "screen.temptradeoffs.negative");
        int color = positive ? 0xFF65D96B : 0xFFFF6B6B;
        g.fill(x, y + 6, x + 70, y + 7, color);
        g.drawString(font, text, x + 78, y + 1, color);
    }

    private void drawRow(GuiGraphics g, Row row, int x, int y, int w, int mouseX, int mouseY) {
        int bg = row.positive ? 0x441E5A2A : 0x444F2020;
        int accent = row.positive ? 0xFF55CC66 : 0xFFFF6666;
        g.fill(x, y, x + w, y + ROW_H - 3, bg);
        g.fill(x, y, x + 3, y + ROW_H - 3, accent);

        String name = getName(row.modifier);
        String value = getValue(row.modifier);
        int valueWidth = value.isEmpty() ? 0 : font.width(value);
        int maxNameWidth = w - 24 - valueWidth;
        name = trim(name, maxNameWidth);

        g.drawString(font, name, x + 10, y + 4, 0xFFFFFF);
        if (!value.isEmpty()) {
            g.drawString(font, value, x + w - valueWidth - 10, y + 4, accent);
        }

        String type = typeLabel(row.modifier.type());
        g.drawString(font, type, x + 10, y + 15, 0xFF999999);

    }

    private String typeLabel(Tradeoff.ModifierType type) {
        return switch (type) {
            case MOB_EFFECT -> Component.translatable("screen.temptradeoffs.type.effect").getString();
            case ATTRIBUTE -> Component.translatable("screen.temptradeoffs.type.attribute").getString();
            case MNS_STAT -> Component.translatable("screen.temptradeoffs.type.mns").getString();
        };
    }

    private String trim(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String suffix = "…";
        while (!value.isEmpty() && font.width(value + suffix) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private String getName(TTPackets.ModifierView m) {
        if (m.type() == Tradeoff.ModifierType.MNS_STAT) {
            return m.displayName().isEmpty() ? m.id().toString() : m.displayName();
        }
        if (m.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = BuiltInRegistries.MOB_EFFECT.get(m.id());
            if (effect == null) return m.id().toString();
            String name = Component.translatable(effect.getDescriptionId()).getString();
            return m.amplifier() > 0 ? name + " " + (m.amplifier() + 1) : name;
        }
        Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(m.id());
        return attribute == null ? m.id().toString() : Component.translatable(attribute.getDescriptionId()).getString();
    }

    private String getValue(TTPackets.ModifierView m) {
        if (m.type() == Tradeoff.ModifierType.MNS_STAT) return m.displayValue();
        if (m.type() == Tradeoff.ModifierType.MOB_EFFECT) return Component.translatable("screen.temptradeoffs.permanent_short").getString();
        return String.format(java.util.Locale.ROOT, "%+.1f%%", m.amount() * 100.0);
    }

    private List<net.minecraft.util.FormattedCharSequence> tooltip(TTPackets.ModifierView m) {
        List<net.minecraft.util.FormattedCharSequence> out = new ArrayList<>();
        out.add(Component.literal(getName(m)).getVisualOrderText());
        if (!m.description().isEmpty()) out.add(Component.literal(m.description()).getVisualOrderText());

        switch (m.type()) {
            case MOB_EFFECT -> out.add(Component.translatable("screen.temptradeoffs.tooltip.effect").getVisualOrderText());
            case ATTRIBUTE -> out.add(Component.translatable("screen.temptradeoffs.tooltip.attribute").getVisualOrderText());
            case MNS_STAT -> out.add(Component.translatable("screen.temptradeoffs.tooltip.mns").getVisualOrderText());
        }

        String value = getValue(m);
        if (!value.isEmpty()) out.add(Component.literal(value).getVisualOrderText());
        out.add(Component.literal(m.id().toString()).getVisualOrderText());
        return out;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scroll = (int) Math.max(0, Math.min(maxScroll, scroll - delta * ROW_H));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
