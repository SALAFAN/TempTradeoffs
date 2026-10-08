package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public class DiagnosticScreen extends Screen {
    private final List<TTPackets.DiagnosticView> diagnostics;
    private int scroll;

    public DiagnosticScreen(List<TTPackets.DiagnosticView> diagnostics) {
        super(Component.translatable("screen.temptradeoffs.diagnostic_title"));
        this.diagnostics = diagnostics;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.temptradeoffs.diagnostic_refresh"),
                b -> TTPackets.CHANNEL.sendToServer(new TTPackets.RequestDiagnosticsPacket()))
                .bounds(width / 2 - 100, height - 52, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 5, height - 52, 95, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.diagnostic_hint"), width / 2, 26, 0xAAAAAA);

        int top = 48, bottom = height - 62;
        int rowH = 42;
        int maxScroll = Math.max(0, diagnostics.size() * rowH - (bottom - top));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        g.enableScissor(10, top, width - 10, bottom);
        int y = top - scroll;
        for (TTPackets.DiagnosticView d : diagnostics) {
            int bg = d.active() ? 0x44306030 : 0x44403030;
            g.fill(12, y, width - 12, y + rowH - 3, bg);
            String name = Component.translatable(d.name()).getString();
            g.drawString(font, (d.active() ? "✓ " : "✕ ") + name, 20, y + 4, d.active() ? 0x66FF66 : 0xFF6666);
            g.drawString(font, Component.translatable("screen.temptradeoffs.diagnostic.expected", d.expected()), 30, y + 16, 0xCCCCCC);
            g.drawString(font, Component.translatable("screen.temptradeoffs.diagnostic.actual", d.actual()), 30, y + 28, 0xCCCCCC);
            y += rowH;
        }
        g.disableScissor();

        if (diagnostics.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.diagnostic_none"), width / 2, top + 20, 0xFFCC66);
        }
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, diagnostics.size() * 42 - (height - 62 - 48));
        if (max > 0) {
            scroll = (int)Math.max(0, Math.min(max, scroll - delta * 42));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }
}
