package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the current-choice button only to the player's normal inventory. */
@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class HudButton {
    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!TTConfig.HUD_BUTTON.get()) return;
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;

        // Absolute screen coordinates are intentional: this keeps the button in
        // the same upper-left HUD area where FTB Quests-style inventory buttons
        // are normally displayed, while the event itself guarantees that it only
        // exists while the normal player inventory is open.
        int x = 6;
        int y = 6;

        FishButton button = new FishButton(
                x, y, 132, 20,
                Component.translatable("screen.temptradeoffs.hud_button"),
                b -> ClientScreenOpener.openInfo()
        );
        button.setTooltip(Tooltip.create(Component.translatable("screen.temptradeoffs.hud_button.tooltip")));
        event.addListener(button);
    }

    private HudButton() {}
}
