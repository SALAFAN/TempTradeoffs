package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.Minecraft;
import java.util.List;

public final class ClientDiagnostics {
    public static void open(List<TTPackets.DiagnosticView> diagnostics) {
        Minecraft.getInstance().setScreen(new DiagnosticScreen(diagnostics));
    }
    private ClientDiagnostics() {}
}
