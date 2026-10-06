package com.warg.temptradeoffs.server;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.common.TradeoffPool;
import com.warg.temptradeoffs.config.TTConfig;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Random;

public final class TradeoffManager {
    private static final String ROOT = "TempTradeoffs";
    private static final String LAST_DAY = "LastDay";
    private static final String LAST_OFFER = "LastOfferDay";
    private static final String CURRENT = "Current";
    private static final String MANAGED_EFFECTS = "ManagedEffects";

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer sp)) return;
        if (TTConfig.NEW_DAY.get() && sp.level().getDayTime() % 24000L == 0L) {
            long day = sp.level().getDayTime() / 24000L;
            CompoundTag tag = data(sp);
            if (tag.getLong(LAST_DAY) != day) {
                tag.putLong(LAST_DAY, day);
                if (canOffer(sp, day)) openChoice(sp, false);
            }
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !TTConfig.ON_LOGIN.get()) return;
        if (!data(sp).contains(CURRENT)) openChoice(sp, true);
    }

    public static void onLevelUp(ServerPlayer sp) {
        if (TTConfig.LEVEL_UP.get()) openChoice(sp, true);
    }

    private static boolean canOffer(ServerPlayer sp, long day) {
        int cd = TTConfig.COOLDOWN_DAYS.get();
        return cd <= 0 || day - data(sp).getLong(LAST_OFFER) >= cd;
    }

    public static void openChoice(ServerPlayer sp, boolean force) {
        long day = sp.level().getDayTime() / 24000L;
        if (!force && !canOffer(sp, day)) return;

        List<Tradeoff> choices = TradeoffPool.randomChoices(
                new Random(sp.getRandom().nextLong()),
                TTConfig.CHOICES.get()
        );

        CompoundTag cur = new CompoundTag();
        for (int i = 0; i < choices.size(); i++) cur.putString("C" + i, choices.get(i).id());
        data(sp).put(CURRENT, cur);
        data(sp).putLong(LAST_OFFER, day);

        int duration = TTConfig.DURATION_MINUTES.get() * 60 * 20;
        TTPackets.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new TTPackets.OpenChoicePacket(choices.stream()
                        .map(c -> TTPackets.view(c, duration))
                        .toList())
        );
    }

    public static void select(ServerPlayer sp, int index) {
        CompoundTag root = data(sp);
        if (index < 0 || !root.contains(CURRENT)) return;

        CompoundTag current = root.getCompound(CURRENT);
        if (index >= current.size()) return;

        String id = current.getString("C" + index);
        Tradeoff chosen = TradeoffPool.ALL.stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElse(null);
        if (chosen == null) return;

        // Remove only effects previously applied by this mod.
        clearManagedEffects(sp);

        int duration = TTConfig.DURATION_MINUTES.get() * 60 * 20;
        ListTag managed = new ListTag();
        long gameTime = sp.level().getGameTime();

        for (var spec : chosen.positive()) applyManagedEffect(sp, spec, duration, gameTime, managed);
        for (var spec : chosen.negative()) applyManagedEffect(sp, spec, duration, gameTime, managed);

        root.put(MANAGED_EFFECTS, managed);
        root.remove(CURRENT);
        sp.displayClientMessage(Component.translatable(chosen.titleKey()), true);
    }

    private static void applyManagedEffect(ServerPlayer sp, Tradeoff.EffectSpec spec, int duration, long gameTime, ListTag managed) {
        MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(spec.effect());
        if (effect == null) return;

        CompoundTag entry = new CompoundTag();
        entry.putString("Id", spec.effect().toString());
        entry.putInt("AppliedAmplifier", spec.amplifier());
        entry.putInt("AppliedDuration", duration);
        entry.putLong("AppliedGameTime", gameTime);

        MobEffectInstance previous = sp.getEffect(effect);
        if (previous != null) {
            CompoundTag previousTag = new CompoundTag();
            previous.save(previousTag);
            entry.put("Previous", previousTag);
        }

        sp.addEffect(new MobEffectInstance(effect, duration, spec.amplifier(), false, true, true));
        managed.add(entry);
    }

    private static void clearManagedEffects(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(MANAGED_EFFECTS)) return;

        ListTag managed = root.getList(MANAGED_EFFECTS, 10);
        long now = sp.level().getGameTime();

        for (int i = 0; i < managed.size(); i++) {
            CompoundTag entry = managed.getCompound(i);
            ResourceLocation id;
            try {
                id = new ResourceLocation(entry.getString("Id"));
            } catch (Exception ignored) {
                continue;
            }

            MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect == null) continue;

            MobEffectInstance current = sp.getEffect(effect);
            if (current == null) continue;

            long elapsed = Math.max(0L, now - entry.getLong("AppliedGameTime"));
            int expectedRemaining = Math.max(0, entry.getInt("AppliedDuration") - (int) Math.min(Integer.MAX_VALUE, elapsed));
            int appliedAmplifier = entry.getInt("AppliedAmplifier");

            // If another mod/player changed this effect, leave it untouched.
            // Our own effect should have the same amplifier and approximately the expected remaining duration.
            boolean ours = current.getAmplifier() == appliedAmplifier
                    && Math.abs(current.getDuration() - expectedRemaining) <= 5;

            if (!ours) continue;

            sp.removeEffect(effect);

            if (entry.contains("Previous")) {
                MobEffectInstance previous = MobEffectInstance.load(entry.getCompound("Previous"));
                if (previous != null) sp.addEffect(previous);
            }
        }

        root.remove(MANAGED_EFFECTS);
    }

    private static CompoundTag data(ServerPlayer sp) {
        CompoundTag persistent = sp.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    private TradeoffManager() {}
}
