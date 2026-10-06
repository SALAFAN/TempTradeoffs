package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "temptradeoffs", value = Dist.CLIENT)
public final class ClientScreenOpener {
    private static List<TTPackets.ChoiceView> pendingChoices;

    public static void open(List<TTPackets.ChoiceView> choices) {
        pendingChoices = List.copyOf(choices);
        forceOpen();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingChoices == null) return;
        forceOpen();
    }

    private static void forceOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TradeoffScreen) return;

        Screen parent = mc.screen;
        mc.setScreen(new TradeoffScreen(pendingChoices, parent));
    }

    public static void clearPending() {
        pendingChoices = null;
    }

    private ClientScreenOpener() {}
}
