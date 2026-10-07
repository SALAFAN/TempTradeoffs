package com.warg.temptradeoffs.network;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.server.TradeoffManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
        CHANNEL.registerMessage(id++, RequestDiagnosticsPacket.class, RequestDiagnosticsPacket::encode, RequestDiagnosticsPacket::decode, RequestDiagnosticsPacket::handle);
        CHANNEL.registerMessage(id++, SyncDiagnosticsPacket.class, SyncDiagnosticsPacket::encode, SyncDiagnosticsPacket::decode, SyncDiagnosticsPacket::handle);
    }

    public record ModifierView(
            Tradeoff.ModifierType type,
            ResourceLocation id,
            int amplifier,
            double amount,
            AttributeModifier.Operation operation,
            int weight,
            String mnsModType,
            String displayName,
            String description,
            String displayValue
    ) {}
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

    public record DiagnosticView(
            Tradeoff.ModifierType type,
            ResourceLocation id,
            String name,
            String expected,
            String actual,
            boolean active
    ) {}

    public record RequestDiagnosticsPacket() {
        static void encode(RequestDiagnosticsPacket p, FriendlyByteBuf b) {}
        static RequestDiagnosticsPacket decode(FriendlyByteBuf b) { return new RequestDiagnosticsPacket(); }
        static void handle(RequestDiagnosticsPacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            c.enqueueWork(() -> { if (c.getSender() != null) TradeoffManager.sendDiagnostics(c.getSender()); });
            c.setPacketHandled(true);
        }
    }

    public record SyncDiagnosticsPacket(List<DiagnosticView> diagnostics) {
        static void encode(SyncDiagnosticsPacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.diagnostics.size());
            for (DiagnosticView d : p.diagnostics) {
                b.writeEnum(d.type());
                b.writeResourceLocation(d.id());
                b.writeUtf(d.name());
                b.writeUtf(d.expected());
                b.writeUtf(d.actual());
                b.writeBoolean(d.active());
            }
        }
        static SyncDiagnosticsPacket decode(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<DiagnosticView> out = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                out.add(new DiagnosticView(b.readEnum(Tradeoff.ModifierType.class), b.readResourceLocation(),
                        b.readUtf(), b.readUtf(), b.readUtf(), b.readBoolean()));
            }
            return new SyncDiagnosticsPacket(out);
        }
        static void handle(SyncDiagnosticsPacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> c.enqueueWork(() -> com.warg.temptradeoffs.client.ClientDiagnostics.open(p.diagnostics)));
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

    private static ModifierView modifierView(Tradeoff.ModifierSpec x) {
        String name = "";
        String description = "";
        String value = "";
        if (x.type() == Tradeoff.ModifierType.MNS_STAT) {
            for (var stat : com.warg.temptradeoffs.common.MnsStatCatalog.all()) {
                if (stat.id().equals(x.id())) {
                    name = stat.name();
                    description = stat.description();
                    value = (x.amount() >= 0 ? "+" : "") + trimNumber(x.amount()) + (stat.percent() ? "%" : "");
                    break;
                }
            }
        }
        int amplifier = x.amplifier();
        if (x.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            int level = (int) Math.max(1, Math.min(255, Math.rint(x.amount())));
            amplifier = level - 1;
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(x.id());
            if (effect != null) name = Component.translatable(effect.getDescriptionId()).getString();
            value = Integer.toString(level);
        } else if (x.type() == Tradeoff.ModifierType.ATTRIBUTE) {
            var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(x.id());
            if (attribute != null) name = net.minecraft.network.chat.Component.translatable(attribute.getDescriptionId()).getString();
            value = String.format(java.util.Locale.ROOT, "%+.1f%%", x.amount() * 100.0);
        }
        return new ModifierView(x.type(), x.id(), amplifier, x.amount(), x.operation(), x.weight(),
                x.mnsModType(), name, description, value);
    }

    private static String trimNumber(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void writeChoices(FriendlyByteBuf b, List<ChoiceView> choices) { b.writeVarInt(choices.size()); for (ChoiceView c : choices) writeChoice(b, c); }
    private static List<ChoiceView> readChoices(FriendlyByteBuf b) { int n = b.readVarInt(); List<ChoiceView> out = new ArrayList<>(); for (int i=0;i<n;i++) out.add(readChoice(b)); return out; }
    private static void writeChoice(FriendlyByteBuf b, ChoiceView c) {
        b.writeUtf(c.choiceId); b.writeUtf(c.titleKey); writeModifiers(b, c.positive); writeModifiers(b, c.negative);
    }
    private static ChoiceView readChoice(FriendlyByteBuf b) { return new ChoiceView(b.readUtf(), b.readUtf(), readModifiers(b), readModifiers(b)); }
    private static void writeModifiers(FriendlyByteBuf b, List<ModifierView> list) {
        b.writeVarInt(list.size());
        for (ModifierView m : list) { b.writeEnum(m.type()); b.writeResourceLocation(m.id()); b.writeVarInt(m.amplifier()); b.writeDouble(m.amount()); b.writeEnum(m.operation()); b.writeVarInt(m.weight()); b.writeUtf(m.mnsModType()); b.writeUtf(m.displayName()); b.writeUtf(m.description()); b.writeUtf(m.displayValue()); }
    }
    private static List<ModifierView> readModifiers(FriendlyByteBuf b) {
        int n = b.readVarInt(); List<ModifierView> out = new ArrayList<>();
        for (int i=0;i<n;i++) out.add(new ModifierView(b.readEnum(Tradeoff.ModifierType.class), b.readResourceLocation(), b.readVarInt(), b.readDouble(), b.readEnum(AttributeModifier.Operation.class), b.readVarInt(), b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf()));
        return out;
    }
    private TTPackets() {}
}
