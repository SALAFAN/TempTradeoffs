package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;

public final class ClientState {
    private static TTPackets.ChoiceView current;
    public static void setCurrent(TTPackets.ChoiceView value) { current = value; }
    public static TTPackets.ChoiceView getCurrent() { return current; }
    private ClientState() {}
}
