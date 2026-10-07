package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = TempTradeoffs.MODID, value = Dist.CLIENT)
public final class ClientScreenOpener {
    private static boolean pending;
    private static Screen previousScreen;

    public static void open(List<TTPackets.ChoiceView> choices) {
        Minecraft mc = Minecraft.getInstance();

        previousScreen = mc.screen;
        pending = true;

        mc.setScreen(new TradeoffScreen(choices));
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!pending) {
            return;
        }

        // The tradeoff window has priority over every other GUI.
        // This specifically prevents Midnight Thoughts from replacing it
        // when its daily statistics screen opens immediately after sleeping.
        if (!(event.getScreen() instanceof TradeoffScreen)) {
            event.setCanceled(true);
        }
    }

    public static void closeChoice() {
        Minecraft mc = Minecraft.getInstance();
        Screen restore = previousScreen;

        pending = false;
        previousScreen = null;

        mc.setScreen(restore);
    }

    public static boolean isPending() {
        return pending;
    }

    private ClientScreenOpener() {}
}
