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

        contentTop = PANEL_TOP + 98;
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
        addRenderableWidget(Button.builder(
                Component.translatable("screen.temptradeoffs.diagnostic_button"),
                b -> TTPackets.CHANNEL.sendToServer(new TTPackets.RequestDiagnosticsPacket())
        ).bounds(width / 2 - 140, height - 54, 280, 20).build());
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
        g.fill(left, top, right, top + 3, current.rarity().color());
        g.fill(left, bottom - 1, right, bottom, 0xFF555555);
        g.fill(left, top, left + 1, bottom, 0xFF555555);
        g.fill(right - 1, top, right, bottom, 0xFF555555);

        g.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable(current.rarity().translationKey()), width / 2, top + 27, current.rarity().color());
        g.drawCenteredString(font, Component.translatable(current.titleKey()), width / 2, top + 43, 0xFFD58A3A);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.current_hint"), width / 2, top + 59, 0xAAAAAA);
        if (minecraft != null && minecraft.level != null) {
            long dayTime = minecraft.level.getDayTime();
            long nextDay = ((dayTime / 24000L) + 1L) * 24000L;
            long remaining = Math.max(0L, nextDay - dayTime);
            g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.next_day", formatGameTime(remaining)), width / 2, top + 73, 0xFFD0A85A);
        }

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
            g.renderTooltip(font, ModifierTooltip.lines(hoveredModifier), mouseX, mouseY);
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

    private String formatGameTime(long ticks) {
        long totalSeconds = (ticks + 19L) / 20L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds);
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

        String name = ModifierTooltip.name(row.modifier);
        String value = ModifierTooltip.value(row.modifier);
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

    private String getName(TTPackets.ModifierView m) { return ModifierTooltip.name(m); }
    private String getValue(TTPackets.ModifierView m) { return ModifierTooltip.value(m); }

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
