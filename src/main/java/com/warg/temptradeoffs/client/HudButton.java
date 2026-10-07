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
        // When FTB Library is present, its native sidebar button is used instead.
        // FTB Library owns its drag/edit mode, so we do not create a competing
        // widget which can conflict with inventory/Curios mouse handling.
        if (net.minecraftforge.fml.ModList.get().isLoaded("ftblibrary")) return;
        Screen screen = event.getScreen();
        if (!isInventoryScreen(screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;

        // Use one fixed default position on every supported inventory screen: immediately
        // to the right of the standard two-button FTB block. After the player drags it,
        // the manually saved coordinates are used instead.
        int autoX = findFtbRightEdge(event) + 4;
        int autoY = findFtbTop(event);
        boolean saved = TTConfig.HUD_POSITION_SET.get() && TTConfig.HUD_X.get() >= 0 && TTConfig.HUD_Y.get() >= 0;
        int x = saved ? TTConfig.HUD_X.get() : autoX;
        int y = saved ? TTConfig.HUD_Y.get() : autoY;
        x = Math.max(0, Math.min(screen.width - 20, x));
        y = Math.max(0, Math.min(screen.height - 20, y));

        FishButton button = new FishButton(x, y, 20, 20,
                Component.translatable("screen.temptradeoffs.hud_button.tooltip"),
                b -> ClientScreenOpener.openInfo());
        event.addListener(button);
    }

    private static int findFtbRightEdge(ScreenEvent.Init.Post event) {
        // FTB Library's sidebar is rendered by its own overlay system, so its
        // buttons are not necessarily present in Screen#getChildren() and
        // cannot reliably be discovered as AbstractWidgets here. The previous
        // heuristic therefore fell back to (4,4), which put the fish in the
        // screen corner. The 1.20.1 FTB sidebar occupies the top-left inventory
        // area; the Teams/Quests pair uses two 20px slots with a small gap.
        // Put the fish immediately to their right.
        return 48;
    }

    private static int findFtbTop(ScreenEvent.Init.Post event) {
        return 4;
    }

    private static boolean isInventoryScreen(Screen screen) {
        if (screen instanceof InventoryScreen) return true;
        String name = screen.getClass().getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("curios") && name.contains("screen");
    }

    private HudButton() {}
}
