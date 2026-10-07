package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
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

        // FTB Library's sidebar is not reliably exposed through Screen.children();
        // it can be mounted in its own widget hierarchy. Use one fixed screen-space
        // anchor for both vanilla InventoryScreen and Curios so the button never jumps.
        // On the target FTB layout this is immediately to the right of the Teams/Quests block.
        int x = 80;
        int y = 25;
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
