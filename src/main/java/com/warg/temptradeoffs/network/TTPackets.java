package com.warg.temptradeoffs.network;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.server.TradeoffManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class TTPackets {
    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TempTradeoffs.MODID, "network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, OpenChoicePacket.class, OpenChoicePacket::encode, OpenChoicePacket::decode, OpenChoicePacket::handle);
        CHANNEL.registerMessage(id++, SelectChoicePacket.class, SelectChoicePacket::encode, SelectChoicePacket::decode, SelectChoicePacket::handle);
        CHANNEL.registerMessage(id++, SyncCurrentPacket.class, SyncCurrentPacket::encode, SyncCurrentPacket::decode, SyncCurrentPacket::handle);
        CHANNEL.registerMessage(id++, RequestCurrentPacket.class, RequestCurrentPacket::encode, RequestCurrentPacket::decode, RequestCurrentPacket::handle);
    }

    public record ModifierView(Tradeoff.ModifierType type, ResourceLocation id, int amplifier, double amount, AttributeModifier.Operation operation, int weight) {}
    public record ChoiceView(String choiceId, String titleKey, List<ModifierView> positive, List<ModifierView> negative) {}

    public record OpenChoicePacket(List<ChoiceView> choices) {
        static void encode(OpenChoicePacket p, FriendlyByteBuf b) { writeChoices(b, p.choices); }
        static OpenChoicePacket decode(FriendlyByteBuf b) { return new OpenChoicePacket(readChoices(b)); }
        static void handle(OpenChoicePacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> c.enqueueWork(() -> com.warg.temptradeoffs.client.ClientScreenOpener.open(p.choices)));
            c.setPacketHandled(true);
        }
    }

    public record SyncCurrentPacket(ChoiceView current) {
        static void encode(SyncCurrentPacket p, FriendlyByteBuf b) { b.writeBoolean(p.current != null); if (p.current != null) writeChoice(b, p.current); }
        static SyncCurrentPacket decode(FriendlyByteBuf b) { return new SyncCurrentPacket(b.readBoolean() ? readChoice(b) : null); }
        static void handle(SyncCurrentPacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> c.enqueueWork(() -> com.warg.temptradeoffs.client.ClientState.setCurrent(p.current)));
            c.setPacketHandled(true);
        }
    }

    public record RequestCurrentPacket() {
        static void encode(RequestCurrentPacket p, FriendlyByteBuf b) {}
        static RequestCurrentPacket decode(FriendlyByteBuf b) { return new RequestCurrentPacket(); }
        static void handle(RequestCurrentPacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            c.enqueueWork(() -> { if (c.getSender() != null) TradeoffManager.sendCurrent(c.getSender()); });
            c.setPacketHandled(true);
        }
    }

    public record SelectChoicePacket(int index) {
        static void encode(SelectChoicePacket p, FriendlyByteBuf b) { b.writeVarInt(p.index); }
        static SelectChoicePacket decode(FriendlyByteBuf b) { return new SelectChoicePacket(b.readVarInt()); }
        static void handle(SelectChoicePacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get(); c.enqueueWork(() -> { if (c.getSender() != null) TradeoffManager.select(c.getSender(), p.index); }); c.setPacketHandled(true);
        }
    }

    public static ChoiceView view(Tradeoff t) {
        return new ChoiceView(t.id(), t.titleKey(), t.positive().stream().map(TTPackets::modifierView).toList(), t.negative().stream().map(TTPackets::modifierView).toList());
    }

    private static ModifierView modifierView(Tradeoff.ModifierSpec x) { return new ModifierView(x.type(), x.id(), x.amplifier(), x.amount(), x.operation(), x.weight()); }

    private static void writeChoices(FriendlyByteBuf b, List<ChoiceView> choices) { b.writeVarInt(choices.size()); for (ChoiceView c : choices) writeChoice(b, c); }
    private static List<ChoiceView> readChoices(FriendlyByteBuf b) { int n = b.readVarInt(); List<ChoiceView> out = new ArrayList<>(); for (int i=0;i<n;i++) out.add(readChoice(b)); return out; }
    private static void writeChoice(FriendlyByteBuf b, ChoiceView c) {
        b.writeUtf(c.choiceId); b.writeUtf(c.titleKey); writeModifiers(b, c.positive); writeModifiers(b, c.negative);
    }
    private static ChoiceView readChoice(FriendlyByteBuf b) { return new ChoiceView(b.readUtf(), b.readUtf(), readModifiers(b), readModifiers(b)); }
    private static void writeModifiers(FriendlyByteBuf b, List<ModifierView> list) {
        b.writeVarInt(list.size());
        for (ModifierView m : list) { b.writeEnum(m.type()); b.writeResourceLocation(m.id()); b.writeVarInt(m.amplifier()); b.writeDouble(m.amount()); b.writeEnum(m.operation()); b.writeVarInt(m.weight()); }
    }
    private static List<ModifierView> readModifiers(FriendlyByteBuf b) {
        int n = b.readVarInt(); List<ModifierView> out = new ArrayList<>();
        for (int i=0;i<n;i++) out.add(new ModifierView(b.readEnum(Tradeoff.ModifierType.class), b.readResourceLocation(), b.readVarInt(), b.readDouble(), b.readEnum(AttributeModifier.Operation.class), b.readVarInt()));
        return out;
    }
    private TTPackets() {}
}
