package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the current-choice fish button to the normal inventory and Curios inventory screens. */
@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class HudButton {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!TTConfig.HUD_BUTTON.get()) return;
        Screen screen = event.getScreen();
        if (!isInventoryScreen(screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;

        // Put the fish immediately to the right of the existing upper-left button block
        // (FTB Teams / FTB Quests / similar mods), rather than at a fixed coordinate.
        int x = 6;
        int y = 6;
        int rightmost = -1;
        int topmost = Integer.MAX_VALUE;
        for (GuiEventListener child : screen.children()) {
            if (!(child instanceof AbstractWidget widget)) continue;
            int wx = widget.getX();
            int wy = widget.getY();
            if (wx < 0 || wy < 0 || wx > screen.width / 2) continue;
            if (wy > 32) continue;
            rightmost = Math.max(rightmost, wx + widget.getWidth());
            topmost = Math.min(topmost, wy);
        }
        if (rightmost >= 0) {
            x = rightmost + 4;
            y = topmost;
        } else if (screen instanceof InventoryScreen inv) {
            x = inv.getGuiLeft() + 4;
            y = inv.getGuiTop() + 4;
        }
        Component tooltip = Component.translatable("screen.temptradeoffs.hud_button.tooltip");
        FishButton button = new FishButton(x, y, 20, 20, tooltip, b -> ClientScreenOpener.openInfo());
        event.addListener(button);
    }

    private static boolean isInventoryScreen(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        // Avoid a hard Curios dependency while still supporting its inventory screen.
        String name = screen.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("curios") && name.contains("screen");
    }

    private HudButton() {}
}
