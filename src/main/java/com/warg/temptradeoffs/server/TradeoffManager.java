package com.warg.temptradeoffs.server;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.common.TradeoffPool;
import com.warg.temptradeoffs.config.TTConfig;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.UUID;
import java.util.Random;

public final class TradeoffManager {
    private static final String ROOT = "TempTradeoffs";
    private static final String LAST_DAY = "LastDay";
    private static final String LAST_OFFER = "LastOfferDay";
    private static final String CURRENT = "Current";
    private static final String APPLIED = "Applied";
    private static final String SNAPSHOTS = "Snapshots";

    private static final String TYPE = "Type";
    private static final String ID = "Id";
    private static final String AMPLIFIER = "Amplifier";
    private static final String AMOUNT = "Amount";
    private static final String OPERATION = "Operation";
    private static final String UUID_KEY = "UUID";
    private static final String START = "Start";
    private static final String END = "End";

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer sp)) {
            return;
        }

        cleanupExpired(sp);

        if (TTConfig.NEW_DAY.get() && sp.level().getDayTime() % 24000L == 0L) {
            long day = sp.level().getDayTime() / 24000L;
            CompoundTag tag = data(sp);

            if (tag.getLong(LAST_DAY) != day) {
                tag.putLong(LAST_DAY, day);
                if (canOffer(sp, day)) {
                    openChoice(sp, false);
                }
            }
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !TTConfig.ON_LOGIN.get()) {
            return;
        }

        if (!data(sp).contains(CURRENT)) {
            openChoice(sp, true);
        }
    }

    public static void onLevelUp(ServerPlayer sp) {
        if (TTConfig.LEVEL_UP.get()) {
            openChoice(sp, true);
        }
    }

    private static boolean canOffer(ServerPlayer sp, long day) {
        int cd = TTConfig.COOLDOWN_DAYS.get();
        return cd <= 0 || day - data(sp).getLong(LAST_OFFER) >= cd;
    }

    public static void openChoice(ServerPlayer sp, boolean force) {
        long day = sp.level().getDayTime() / 24000L;

        if (!force && !canOffer(sp, day)) {
            return;
        }

        List<Tradeoff> choices = TradeoffPool.randomChoices(
                new Random(sp.getRandom().nextLong()),
                TTConfig.CHOICES.get()
        );

        CompoundTag cur = new CompoundTag();
        for (int i = 0; i < choices.size(); i++) {
            cur.putString("C" + i, choices.get(i).id());
            cur.put("Data" + i, serializeTradeoff(choices.get(i)));
        }

        data(sp).put(CURRENT, cur);
        data(sp).putLong(LAST_OFFER, day);

        int duration = TTConfig.DURATION_MINUTES.get() * 60 * 20;

        TTPackets.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new TTPackets.OpenChoicePacket(
                        choices.stream().map(c -> TTPackets.view(c, duration)).toList()
                )
        );
    }

    public static void select(ServerPlayer sp, int index) {
        CompoundTag root = data(sp);
        if (!root.contains(CURRENT)) {
            return;
        }

        CompoundTag current = root.getCompound(CURRENT);
        String id = current.getString("C" + index);

        if (index < 0 || index >= TTConfig.CHOICES.get()) {
            return;
        }

        CompoundTag serialized = current.getCompound("Data" + index);
        if (serialized.isEmpty()) {
            return;
        }

        chosen = deserializeTradeoff(serialized);
        if (chosen == null) {
            return;
        }

        clearApplied(sp);
        applyTradeoff(sp, chosen);

        root.remove(CURRENT);
        sp.displayClientMessage(Component.translatable(chosen.titleKey()), true);
    }

    private static void applyTradeoff(ServerPlayer sp, Tradeoff chosen) {
        int duration = TTConfig.DURATION_MINUTES.get() * 60 * 20;
        long start = sp.level().getGameTime();
        long end = start + duration;

        ListTag applied = new ListTag();
        ListTag snapshots = new ListTag();

        int index = 0;
        for (Tradeoff.ModifierSpec spec : chosen.positive()) {
            applySpec(sp, spec, start, end, index++, applied, snapshots);
        }
        for (Tradeoff.ModifierSpec spec : chosen.negative()) {
            applySpec(sp, spec, start, end, index++, applied, snapshots);
        }

        CompoundTag root = data(sp);
        root.put(APPLIED, applied);
        root.put(SNAPSHOTS, snapshots);
    }

    private static void applySpec(
            ServerPlayer sp,
            Tradeoff.ModifierSpec spec,
            long start,
            long end,
            int index,
            ListTag applied,
            ListTag snapshots
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE, spec.type().name());
        tag.putString(ID, spec.id().toString());
        tag.putInt(AMPLIFIER, spec.amplifier());
        tag.putDouble(AMOUNT, spec.amount());
        tag.putString(OPERATION, spec.operation().name());
        tag.putLong(START, start);
        tag.putLong(END, end);

        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect == null) {
                return;
            }

            MobEffectInstance old = sp.getEffect(effect);
            if (old != null) {
                CompoundTag snapshot = new CompoundTag();
                snapshot.putString(ID, spec.id().toString());
                snapshot.putInt(AMPLIFIER, old.getAmplifier());
                snapshot.putInt("Duration", old.getDuration());
                snapshot.putBoolean("Ambient", old.isAmbient());
                snapshot.putBoolean("Visible", old.isVisible());
                snapshot.putBoolean("Icon", old.showIcon());
                snapshots.add(snapshot);
            }

            sp.addEffect(new MobEffectInstance(
                    effect,
                    (int) (end - start),
                    spec.amplifier(),
                    false,
                    true,
                    true
            ));
        } else {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute == null) {
                return;
            }

            AttributeInstance instance = sp.getAttribute(attribute);
            if (instance == null) {
                return;
            }

            UUID uuid = UUID.nameUUIDFromBytes(
                    ("temptradeoffs:" + sp.getUUID() + ":" + start + ":" + index + ":" + spec.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            AttributeModifier modifier = new AttributeModifier(
                    uuid,
                    "TempTradeoffs",
                    spec.amount(),
                    spec.operation()
            );

            // Remove only our own modifier with this UUID, never another mod's modifier.
            instance.removeModifier(uuid);
            instance.addTransientModifier(modifier);

            tag.putUUID(UUID_KEY, uuid);
        }

        applied.add(tag);
    }

    private static void cleanupExpired(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(APPLIED)) {
            return;
        }

        ListTag applied = root.getList(APPLIED, Tag.TAG_COMPOUND);
        long now = sp.level().getGameTime();

        boolean anyAlive = false;

        for (int i = applied.size() - 1; i >= 0; i--) {
            CompoundTag tag = applied.getCompound(i);
            long end = tag.getLong(END);

            if (now < end) {
                anyAlive = true;
                continue;
            }

            removeAppliedEntry(sp, tag);
            applied.remove(i);
        }

        if (!anyAlive && applied.isEmpty()) {
            restoreSnapshots(sp);
            root.remove(APPLIED);
            root.remove(SNAPSHOTS);
        }
    }

    private static void removeAppliedEntry(ServerPlayer sp, CompoundTag tag) {
        Tradeoff.ModifierType type = Tradeoff.ModifierType.valueOf(tag.getString(TYPE));
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID));

        if (id == null) {
            return;
        }

        if (type == Tradeoff.ModifierType.ATTRIBUTE) {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(id);
            if (attribute == null) {
                return;
            }

            AttributeInstance instance = sp.getAttribute(attribute);
            if (instance != null && tag.hasUUID(UUID_KEY)) {
                instance.removeModifier(tag.getUUID(UUID_KEY));
            }
            return;
        }

        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) {
            return;
        }

        MobEffectInstance current = sp.getEffect(effect);
        if (current == null) {
            return;
        }

        long start = tag.getLong(START);
        long end = tag.getLong(END);
        int expected = (int) Math.max(0, end - sp.level().getGameTime());

        // If another mod changed/replaced this effect, leave it alone.
        // Otherwise remove our copy and restore the previous instance below.
        if (current.getAmplifier() == tag.getInt(AMPLIFIER)
                && Math.abs(current.getDuration() - expected) <= 2) {
            sp.removeEffect(effect);
        }
    }

    private static void clearApplied(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(APPLIED)) {
            return;
        }

        ListTag applied = root.getList(APPLIED, Tag.TAG_COMPOUND);

        for (int i = applied.size() - 1; i >= 0; i--) {
            removeAppliedEntry(sp, applied.getCompound(i));
        }

        restoreSnapshots(sp);
        root.remove(APPLIED);
        root.remove(SNAPSHOTS);
    }

    private static void restoreSnapshots(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(SNAPSHOTS)) {
            return;
        }

        ListTag snapshots = root.getList(SNAPSHOTS, Tag.TAG_COMPOUND);

        for (int i = 0; i < snapshots.size(); i++) {
            CompoundTag snapshot = snapshots.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(snapshot.getString(ID));
            if (id == null) {
                continue;
            }

            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect == null) {
                continue;
            }

            // Only restore if there isn't another mod's replacement already active.
            if (!sp.hasEffect(effect)) {
                sp.addEffect(new MobEffectInstance(
                        effect,
                        snapshot.getInt("Duration"),
                        snapshot.getInt(AMPLIFIER),
                        snapshot.getBoolean("Ambient"),
                        snapshot.getBoolean("Visible"),
                        snapshot.getBoolean("Icon")
                ));
            }
        }
    }

    private static CompoundTag data(ServerPlayer sp) {
        CompoundTag persistent = sp.getPersistentData();

        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }

        return persistent.getCompound(ROOT);
    }

    private static CompoundTag serializeTradeoff(Tradeoff t) {
        CompoundTag out = new CompoundTag();
        out.putString("Id", t.id());
        out.putString("Title", t.titleKey());
        out.put("Positive", serializeSide(t.positive()));
        out.put("Negative", serializeSide(t.negative()));
        return out;
    }

    private static ListTag serializeSide(List<Tradeoff.ModifierSpec> list) {
        ListTag out = new ListTag();
        for (Tradeoff.ModifierSpec spec : list) {
            CompoundTag tag = new CompoundTag();
            tag.putString(TYPE, spec.type().name());
            tag.putString(ID, spec.id().toString());
            tag.putInt(AMPLIFIER, spec.amplifier());
            tag.putDouble(AMOUNT, spec.amount());
            tag.putString(OPERATION, spec.operation().name());
            tag.putInt("Weight", spec.weight());
            out.add(tag);
        }
        return out;
    }

    private static Tradeoff deserializeTradeoff(CompoundTag tag) {
        String id = tag.getString("Id");
        String title = tag.getString("Title");

        List<Tradeoff.ModifierSpec> positive = deserializeSide(tag.getList("Positive", Tag.TAG_COMPOUND));
        List<Tradeoff.ModifierSpec> negative = deserializeSide(tag.getList("Negative", Tag.TAG_COMPOUND));

        return new Tradeoff(id, title, positive, negative);
    }

    private static List<Tradeoff.ModifierSpec> deserializeSide(ListTag list) {
        java.util.ArrayList<Tradeoff.ModifierSpec> result = new java.util.ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID));
            if (id == null) {
                continue;
            }

            result.add(new Tradeoff.ModifierSpec(
                    Tradeoff.ModifierType.valueOf(tag.getString(TYPE)),
                    id,
                    tag.getInt(AMPLIFIER),
                    tag.getDouble(AMOUNT),
                    AttributeModifier.Operation.valueOf(tag.getString(OPERATION)),
                    tag.getInt("Weight")
            ));
        }

        return result;
    }

    private TradeoffManager() {}
}
