package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    public static void openInfo() {
        Minecraft mc = Minecraft.getInstance();
        TTPackets.ChoiceView current = ClientState.getCurrent();
        if (current != null) mc.setScreen(new TradeoffInfoScreen(current));
        else TTPackets.CHANNEL.sendToServer(new TTPackets.RequestCurrentPacket());
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (pending && !(event.getScreen() instanceof TradeoffScreen)) event.setCanceled(true);
    }

    public static void closeChoice() {
        Minecraft mc = Minecraft.getInstance();
        Screen restore = previousScreen;
        pending = false;
        previousScreen = null;
        mc.setScreen(restore);
    }

    public static boolean isPending() { return pending; }
    private ClientScreenOpener() {}
}
