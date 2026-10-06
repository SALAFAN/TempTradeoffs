package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="temptradeoffs", value=Dist.CLIENT)
public final class ClientScreenOpener {
    public static void open(java.util.List<TTPackets.ChoiceView> choices) {
        Minecraft.getInstance().setScreen(new TradeoffScreen(choices));
    }
    private ClientScreenOpener() {}
}
