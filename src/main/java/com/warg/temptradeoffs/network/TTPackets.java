package com.warg.temptradeoffs.network;

import com.warg.temptradeoffs.TempTradeoffs;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.server.TradeoffManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class TTPackets {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TempTradeoffs.MODID, "network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, OpenChoicePacket.class,
                OpenChoicePacket::encode, OpenChoicePacket::decode, OpenChoicePacket::handle);
        CHANNEL.registerMessage(id++, SelectChoicePacket.class,
                SelectChoicePacket::encode, SelectChoicePacket::decode, SelectChoicePacket::handle);
    }

    public record EffectView(ResourceLocation id, int amplifier) {}

    public record ChoiceView(
            String choiceId,
            String titleKey,
            List<EffectView> positive,
            List<EffectView> negative,
            int durationTicks
    ) {}

    public record OpenChoicePacket(List<ChoiceView> choices) {
        static void encode(OpenChoicePacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.choices.size());
            for (ChoiceView c : p.choices) {
                b.writeUtf(c.choiceId);
                b.writeUtf(c.titleKey);
                b.writeVarInt(c.durationTicks);
                writeEffects(b, c.positive);
                writeEffects(b, c.negative);
            }
        }

        static OpenChoicePacket decode(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<ChoiceView> cs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                String choiceId = b.readUtf();
                String titleKey = b.readUtf();
                int durationTicks = b.readVarInt();
                List<EffectView> positive = readEffects(b);
                List<EffectView> negative = readEffects(b);
                cs.add(new ChoiceView(choiceId, titleKey, positive, negative, durationTicks));
            }
            return new OpenChoicePacket(cs);
        }

        static void writeEffects(FriendlyByteBuf b, List<EffectView> es) {
            b.writeVarInt(es.size());
            for (EffectView e : es) {
                b.writeResourceLocation(e.id);
                b.writeVarInt(e.amplifier);
            }
        }

        static List<EffectView> readEffects(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<EffectView> r = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                r.add(new EffectView(b.readResourceLocation(), b.readVarInt()));
            }
            return r;
        }

        static void handle(OpenChoicePacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> c.enqueueWork(() -> com.warg.temptradeoffs.client.ClientScreenOpener.open(p.choices))
            );
            c.setPacketHandled(true);
        }
    }

    public record SelectChoicePacket(int index) {
        static void encode(SelectChoicePacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.index);
        }

        static SelectChoicePacket decode(FriendlyByteBuf b) {
            return new SelectChoicePacket(b.readVarInt());
        }

        static void handle(SelectChoicePacket p, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context c = sup.get();
            c.enqueueWork(() -> {
                var sp = c.getSender();
                if (sp != null) TradeoffManager.select(sp, p.index);
            });
            c.setPacketHandled(true);
        }
    }

    public static ChoiceView view(Tradeoff t, int durationTicks) {
        return new ChoiceView(
                t.id(),
                t.titleKey(),
                t.positive().stream().map(x -> new EffectView(x.effect(), x.amplifier())).toList(),
                t.negative().stream().map(x -> new EffectView(x.effect(), x.amplifier())).toList(),
                durationTicks
        );
    }

    private TTPackets() {}
}
