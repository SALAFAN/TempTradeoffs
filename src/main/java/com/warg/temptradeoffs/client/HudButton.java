package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class HudButton {
    private static final int X = 4, Y = 4, W = 104, H = 20;

    @SubscribeEvent
    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!com.warg.temptradeoffs.config.TTConfig.HUD_BUTTON.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;
        if (ClientState.getCurrent() == null) return;
        GuiGraphics g = event.getGuiGraphics();
        g.fill(X, Y, X + W, Y + H, 0xB0000000);
        g.fill(X, Y, X + W, Y + 1, 0xFFFFFFFF);
        g.drawString(mc.font, net.minecraft.network.chat.Component.translatable("screen.temptradeoffs.hud_button"), X + 5, Y + 6, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Pre event) {
        if (!com.warg.temptradeoffs.config.TTConfig.HUD_BUTTON.get() || event.getButton() != 0 || event.getAction() != 1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending() || ClientState.getCurrent() == null) return;
        double sx = mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double sy = mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        double x = mc.mouseHandler.xpos() * sx, y = mc.mouseHandler.ypos() * sy;
        if (x >= X && x <= X + W && y >= Y && y <= Y + H) {
            event.setCanceled(true);
            ClientScreenOpener.openInfo();
        }
    }
    private HudButton() {}
}
