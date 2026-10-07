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
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TempTradeoffs.MODID, "network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(
                id++,
                OpenChoicePacket.class,
                OpenChoicePacket::encode,
                OpenChoicePacket::decode,
                OpenChoicePacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                SelectChoicePacket.class,
                SelectChoicePacket::encode,
                SelectChoicePacket::decode,
                SelectChoicePacket::handle
        );
    }

    public record ModifierView(
            Tradeoff.ModifierType type,
            ResourceLocation id,
            int amplifier,
            double amount,
            AttributeModifier.Operation operation,
            int weight
    ) {}

    public record ChoiceView(
            String choiceId,
            String titleKey,
            List<ModifierView> positive,
            List<ModifierView> negative,
            int durationTicks
    ) {}

    public record OpenChoicePacket(List<ChoiceView> choices) {
        static void encode(OpenChoicePacket p, FriendlyByteBuf b) {
            b.writeVarInt(p.choices.size());

            for (ChoiceView c : p.choices) {
                b.writeUtf(c.choiceId);
                b.writeUtf(c.titleKey);
                b.writeVarInt(c.durationTicks);

                writeModifiers(b, c.positive);
                writeModifiers(b, c.negative);
            }
        }

        static OpenChoicePacket decode(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<ChoiceView> choices = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                String id = b.readUtf();
                String title = b.readUtf();
                int duration = b.readVarInt();
                List<ModifierView> positive = readModifiers(b);
                List<ModifierView> negative = readModifiers(b);
                choices.add(new ChoiceView(id, title, positive, negative, duration));
            }

            return new OpenChoicePacket(choices);
        }

        private static void writeModifiers(FriendlyByteBuf b, List<ModifierView> list) {
            b.writeVarInt(list.size());
            for (ModifierView m : list) {
                b.writeEnum(m.type());
                b.writeResourceLocation(m.id());
                b.writeVarInt(m.amplifier());
                b.writeDouble(m.amount());
                b.writeEnum(m.operation());
                b.writeVarInt(m.weight());
            }
        }

        private static List<ModifierView> readModifiers(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<ModifierView> result = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                result.add(new ModifierView(
                        b.readEnum(Tradeoff.ModifierType.class),
                        b.readResourceLocation(),
                        b.readVarInt(),
                        b.readDouble(),
                        b.readEnum(AttributeModifier.Operation.class),
                        b.readVarInt()
                ));
            }

            return result;
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
                if (sp != null) {
                    TradeoffManager.select(sp, p.index);
                }
            });
            c.setPacketHandled(true);
        }
    }

    public static ChoiceView view(Tradeoff t, int durationTicks) {
        return new ChoiceView(
                t.id(),
                t.titleKey(),
                t.positive().stream().map(TTPackets::modifierView).toList(),
                t.negative().stream().map(TTPackets::modifierView).toList(),
                durationTicks
        );
    }

    private static ModifierView modifierView(Tradeoff.ModifierSpec x) {
        return new ModifierView(
                x.type(),
                x.id(),
                x.amplifier(),
                x.amount(),
                x.operation(),
                x.weight()
        );
    }

    private TTPackets() {}
}
