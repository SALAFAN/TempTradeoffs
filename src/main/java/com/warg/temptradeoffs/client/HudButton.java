package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the current-choice fish button next to the FTB Teams/Quests sidebar. */
@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class HudButton {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!TTConfig.HUD_BUTTON.get()) return;
        Screen screen = event.getScreen();
        if (!isInventoryScreen(screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;

        // Use one fixed default position on every supported inventory screen: immediately
        // to the right of the standard two-button FTB block. After the player drags it,
        // the manually saved coordinates are used instead.
        int autoX = 58;
        int autoY = 4;
        int x = TTConfig.HUD_POSITION_SET.get() ? TTConfig.HUD_X.get() : autoX;
        int y = TTConfig.HUD_POSITION_SET.get() ? TTConfig.HUD_Y.get() : autoY;
        x = Math.max(0, Math.min(screen.width - 20, x));
        y = Math.max(0, Math.min(screen.height - 20, y));

        FishButton button = new FishButton(x, y, 20, 20,
                Component.translatable("screen.temptradeoffs.hud_button.tooltip"),
                b -> ClientScreenOpener.openInfo());
        event.addListener(button);
    }

    private static int findFtbRightEdge(ScreenEvent.Init.Post event) {
        int max = 4;
        boolean found = false;
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget w)) continue;
            String cls = w.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            String msg = w.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
            if ((cls.contains("ftb") || msg.contains("ftb") || msg.contains("quest") || msg.contains("team"))
                    && w.getY() <= 55 && w.getX() < event.getScreen().width / 2) {
                max = Math.max(max, w.getX() + w.getWidth());
                found = true;
            }
        }
        // If FTB uses a nested widget hierarchy, inspect all top-left widgets as a
        // secondary fallback rather than putting the fish in the screen corner.
        if (!found) {
            for (var listener : event.getListenersList()) {
                if (!(listener instanceof AbstractWidget w)) continue;
                if (w.getY() <= 55 && w.getX() < event.getScreen().width / 3 && w.getWidth() <= 80) {
                    max = Math.max(max, w.getX() + w.getWidth());
                }
            }
        }
        return max;
    }

    private static int findFtbTop(ScreenEvent.Init.Post event) {
        int top = 4;
        for (var listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget w)) continue;
            String cls = w.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            String msg = w.getMessage().getString().toLowerCase(java.util.Locale.ROOT);
            if ((cls.contains("ftb") || msg.contains("ftb") || msg.contains("quest") || msg.contains("team"))
                    && w.getY() <= 55 && w.getX() < event.getScreen().width / 2) {
                top = Math.min(top, w.getY());
            }
        }
        return top;
    }

    private static boolean isInventoryScreen(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        String name = screen.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("curios") && name.contains("screen");
    }

    private HudButton() {}
}
