package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Кнопка текущего выбора показывается только внутри обычного инвентаря игрока.
 * Она не является глобальным HUD-оверлеем и потому не появляется в мире,
 * сундуках, других экранах и поверх сторонних интерфейсов.
 */
@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class HudButton {
    private static final int X = 5;
    private static final int Y = 5;
    private static final int W = 118;
    private static final int H = 20;

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!TTConfig.HUD_BUTTON.get()) return;
        if (!(event.getScreen() instanceof InventoryScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ClientScreenOpener.isPending()) return;

        Button button = Button.builder(
                Component.translatable("screen.temptradeoffs.hud_button"),
                b -> ClientScreenOpener.openInfo()
        ).bounds(X, Y, W, H).build();

        event.addListener(button);
    }

    private HudButton() {}
}
