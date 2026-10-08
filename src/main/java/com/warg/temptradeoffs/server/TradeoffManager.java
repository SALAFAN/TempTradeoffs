package com.warg.temptradeoffs.server;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.common.TradeoffPool;
import com.warg.temptradeoffs.config.TTConfig;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class TradeoffManager {
    private static final String ROOT = "TempTradeoffs";
    private static final String LAST_DAY = "LastDay";
    private static final String LAST_OFFER = "LastOfferDay";
    private static final String CURRENT_OFFER = "CurrentOffer";
    private static final String CURRENT_SELECTED = "CurrentSelected";
    private static final String APPLIED = "Applied";
    private static final String SNAPSHOTS = "Snapshots";
    private static final String TYPE = "Type", ID = "Id", AMPLIFIER = "Amplifier", AMOUNT = "Amount", OPERATION = "Operation", UUID_KEY = "UUID";
    private static final String SOURCE = "Source";
    private static final String OWN_SOURCE = "TempTradeoffs";
    private static final String MNS_SOURCE = "MnsSource";
    private static final String MNS_TYPE = "MnsType";

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer sp)) return;

        CompoundTag root = data(sp);
        long day = sp.level().getDayTime() / 24000L;
        long lastSeen = root.getLong(LAST_DAY);

        if (lastSeen != day) {
            root.putLong(LAST_DAY, day);
            if (TTConfig.NEW_DAY.get() && canOffer(sp, day) && !root.contains(CURRENT_OFFER)) openChoice(sp, false);
        }

        if (root.contains(APPLIED) && (sp.tickCount % 20 == 0)) enforceApplied(sp);
        if (isChoosing(sp)) freeze(sp);
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        CompoundTag oldData = oldPlayer.getPersistentData().getCompound(ROOT).copy();
        if (!oldData.isEmpty()) newPlayer.getPersistentData().put(ROOT, oldData);
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        CompoundTag root = data(sp);
        if (root.contains(APPLIED)) {
            enforceApplied(sp);
        } else if (root.contains(CURRENT_SELECTED)) {
            Tradeoff current = deserializeTradeoff(root.getCompound(CURRENT_SELECTED));
            if (current != null) applyTradeoff(sp, current);
        }
        sendCurrent(sp);
        sendOffer(sp);
        // A previously selected tradeoff is persistent. If it exists, do not show
        // the login choice again; the selected modifiers are restored above when needed.
        if (TTConfig.ON_LOGIN.get() && !root.contains(CURRENT_OFFER)
                && (!TTConfig.SKIP_LOGIN_IF_ACTIVE.get() || !root.contains(APPLIED))) {
            openChoice(sp, true);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            enforceApplied(sp);
            sendCurrent(sp);
            sendOffer(sp);
        }
    }

    public static void onLevelChange(PlayerXpEvent.LevelChange event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || event.getLevels() <= 0 || !TTConfig.LEVEL_UP.get()) return;
        if (!isChoosing(sp)) openChoice(sp, true);
    }

    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && isChoosing(sp)) event.setCanceled(true);
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && isChoosing(sp)) event.setCanceled(true);
    }

    public static void onPlayerHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && isChoosing(sp)) event.setCanceled(true);
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer sp && isChoosing(sp)) event.setCanceled(true);
    }

    private static void freeze(ServerPlayer sp) {
        sp.setDeltaMovement(0, 0, 0);
        sp.hurtMarked = true;
        sp.fallDistance = 0;
        // Drop nearby mob targets only periodically; movement/fall protection remains
        // per-tick, while the expensive entity scan is capped to once per second.
        if (sp.tickCount % 20 == 0) {
            for (Mob mob : sp.level().getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(32.0D))) {
                if (mob.getTarget() == sp) mob.setTarget(null);
            }
        }
    }

    private static boolean canOffer(ServerPlayer sp, long day) {
        int cd = TTConfig.COOLDOWN_DAYS.get();
        return cd <= 0 || day - data(sp).getLong(LAST_OFFER) >= cd;
    }

    public static boolean isChoosing(ServerPlayer sp) { return data(sp).contains(CURRENT_OFFER); }

    public static void openInfo(ServerPlayer sp) {
        sendCurrent(sp);
        TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new TTPackets.OpenInfoPacket());
    }

    public static void openChoice(ServerPlayer sp, boolean force) {
        CompoundTag root = data(sp);
        long day = sp.level().getDayTime() / 24000L;
        if (!force && !canOffer(sp, day)) return;
        if (root.contains(CURRENT_OFFER)) return;

        List<Tradeoff> choices = TradeoffPool.randomChoices(new Random(sp.getRandom().nextLong()), TTConfig.CHOICES.get());
        CompoundTag offer = new CompoundTag();
        for (int i = 0; i < choices.size(); i++) offer.put("Data" + i, serializeTradeoff(choices.get(i)));
        root.put(CURRENT_OFFER, offer);
        root.putLong(LAST_OFFER, day);

        TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new TTPackets.OpenChoicePacket(choices.stream().map(TTPackets::view).toList()));
    }

    public static void select(ServerPlayer sp, int index) {
        CompoundTag root = data(sp);
        if (!root.contains(CURRENT_OFFER)) return;
        if (index < 0 || index >= TTConfig.CHOICES.get()) return;

        CompoundTag serialized = root.getCompound(CURRENT_OFFER).getCompound("Data" + index);
        if (serialized.isEmpty()) return;
        Tradeoff chosen = deserializeTradeoff(serialized);
        if (chosen == null) return;

        clearApplied(sp);
        applyTradeoff(sp, chosen);
        root.remove(CURRENT_OFFER);
        root.put(CURRENT_SELECTED, serializeTradeoff(chosen));
        sp.displayClientMessage(Component.translatable(chosen.titleKey()), true);
        sendCurrent(sp);
    }


    private static void sendOffer(ServerPlayer sp) {
        CompoundTag offer = data(sp).getCompound(CURRENT_OFFER);
        if (offer.isEmpty()) return;
        List<TTPackets.ChoiceView> views = new ArrayList<>();
        for (int i = 0; i < TTConfig.CHOICES.get(); i++) {
            CompoundTag serialized = offer.getCompound("Data" + i);
            Tradeoff t = deserializeTradeoff(serialized);
            if (t != null) views.add(TTPackets.view(t));
        }
        if (!views.isEmpty()) TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new TTPackets.OpenChoicePacket(views));
    }

    public static void sendCurrent(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(CURRENT_SELECTED)) {
            TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new TTPackets.SyncCurrentPacket(null));
            return;
        }
        Tradeoff current = deserializeTradeoff(root.getCompound(CURRENT_SELECTED));
        if (current != null) TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new TTPackets.SyncCurrentPacket(TTPackets.view(current)));
    }

    private static void applyTradeoff(ServerPlayer sp, Tradeoff chosen) {
        ListTag applied = new ListTag();
        ListTag snapshots = new ListTag();
        int index = 0;
        for (Tradeoff.ModifierSpec spec : chosen.positive()) applySpec(sp, spec, index++, applied, snapshots);
        for (Tradeoff.ModifierSpec spec : chosen.negative()) applySpec(sp, spec, index++, applied, snapshots);
        CompoundTag root = data(sp);
        root.put(APPLIED, applied);
        root.put(SNAPSHOTS, snapshots);
    }

    private static void applySpec(ServerPlayer sp, Tradeoff.ModifierSpec spec, int index, ListTag applied, ListTag snapshots) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE, spec.type().name()); tag.putString(ID, spec.id().toString());
        tag.putInt(AMPLIFIER, spec.amplifier()); tag.putDouble(AMOUNT, spec.amount()); tag.putString(OPERATION, spec.operation().name()); tag.putString(SOURCE, OWN_SOURCE);

        if (spec.type() == Tradeoff.ModifierType.MNS_STAT) {
            String sourceId = "temptradeoffs:" + sp.getUUID() + ":" + index + ":" + spec.id().getPath();
            tag.putString(MNS_SOURCE, sourceId);
            tag.putString(MNS_TYPE, spec.mnsModType());
            if (MnsBridge.add(sp, spec.id().getPath(), sourceId, (float) spec.amount(), spec.mnsModType())) {
                applied.add(tag);
            }
            return;
        }

        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect == null) return;
            // For status effects the configurable value is the visible level (1..255).
            // The stored amplifier is zero-based, so convert the value here before applying.
            int level = (int) Math.max(1, Math.min(255, Math.rint(spec.amount() > 0 ? spec.amount() : spec.amplifier() + 1)));
            int amplifier = level - 1;
            tag.putInt(AMPLIFIER, amplifier);
            tag.putDouble(AMOUNT, level);
            MobEffectInstance old = sp.getEffect(effect);
            if (old != null) {
                CompoundTag snapshot = new CompoundTag();
                snapshot.putString(ID, spec.id().toString()); snapshot.putInt(AMPLIFIER, old.getAmplifier());
                snapshot.putInt("Duration", old.getDuration()); snapshot.putBoolean("Ambient", old.isAmbient());
                snapshot.putBoolean("Visible", old.isVisible()); snapshot.putBoolean("Icon", old.showIcon());
                snapshots.add(snapshot);
            }
            sp.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, false, true, true));
        } else {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute == null) return;
            AttributeInstance instance = sp.getAttribute(attribute);
            if (instance == null) return;
            UUID uuid = UUID.nameUUIDFromBytes(("temptradeoffs:" + sp.getUUID() + ":" + index + ":" + spec.id()).getBytes(StandardCharsets.UTF_8));
            instance.removeModifier(uuid);
            instance.addTransientModifier(new AttributeModifier(uuid, OWN_SOURCE, spec.amount(), spec.operation()));
            tag.putUUID(UUID_KEY, uuid);
        }
        applied.add(tag);
    }

    private static void enforceApplied(ServerPlayer sp) {
        CompoundTag root = data(sp);
        ListTag applied = root.getList(APPLIED, Tag.TAG_COMPOUND);
        for (int i = 0; i < applied.size(); i++) {
            CompoundTag tag = applied.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID));
            if (id == null) continue;
            Tradeoff.ModifierType type = Tradeoff.ModifierType.valueOf(tag.getString(TYPE));
            if (type == Tradeoff.ModifierType.MNS_STAT) {
                if (tag.contains(MNS_SOURCE)) {
                    MnsBridge.add(sp, id.getPath(), tag.getString(MNS_SOURCE),
                            (float) tag.getDouble(AMOUNT), tag.getString(MNS_TYPE));
                }
                continue;
            }
            if (type == Tradeoff.ModifierType.MOB_EFFECT) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect == null) continue;
                MobEffectInstance current = sp.getEffect(effect);
                if (current == null || current.getAmplifier() != tag.getInt(AMPLIFIER) || !current.isInfiniteDuration()) {
                    sp.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, tag.getInt(AMPLIFIER), false, true, true));
                }
            } else {
                Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(id);
                if (attribute == null || !tag.hasUUID(UUID_KEY)) continue;
                AttributeInstance instance = sp.getAttribute(attribute);
                if (instance != null && instance.getModifier(tag.getUUID(UUID_KEY)) == null) {
                    instance.addTransientModifier(new AttributeModifier(tag.getUUID(UUID_KEY), OWN_SOURCE, tag.getDouble(AMOUNT), AttributeModifier.Operation.valueOf(tag.getString(OPERATION))));
                }
            }
        }
    }

    private static void clearApplied(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(APPLIED)) return;
        ListTag applied = root.getList(APPLIED, Tag.TAG_COMPOUND);
        for (int i = applied.size() - 1; i >= 0; i--) removeAppliedEntry(sp, applied.getCompound(i));
        restoreSnapshots(sp);
        root.remove(APPLIED); root.remove(SNAPSHOTS);
    }

    private static void removeAppliedEntry(ServerPlayer sp, CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID));
        if (id == null) return;
        Tradeoff.ModifierType type = Tradeoff.ModifierType.valueOf(tag.getString(TYPE));
        if (type == Tradeoff.ModifierType.MNS_STAT) {
            if (tag.contains(MNS_SOURCE)) MnsBridge.remove(sp, tag.getString(MNS_SOURCE));
            return;
        }
        if (type == Tradeoff.ModifierType.ATTRIBUTE) {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(id);
            if (attribute != null) {
                AttributeInstance instance = sp.getAttribute(attribute);
                if (instance != null && tag.hasUUID(UUID_KEY)) instance.removeModifier(tag.getUUID(UUID_KEY));
            }
            return;
        }
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) return;
        MobEffectInstance current = sp.getEffect(effect);
        if (current != null && current.getAmplifier() == tag.getInt(AMPLIFIER) && current.isInfiniteDuration()) sp.removeEffect(effect);
    }

    private static void restoreSnapshots(ServerPlayer sp) {
        CompoundTag root = data(sp);
        if (!root.contains(SNAPSHOTS)) return;
        ListTag snapshots = root.getList(SNAPSHOTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < snapshots.size(); i++) {
            CompoundTag s = snapshots.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(s.getString(ID));
            MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect != null && !sp.hasEffect(effect)) {
                sp.addEffect(new MobEffectInstance(effect, s.getInt("Duration"), s.getInt(AMPLIFIER), s.getBoolean("Ambient"), s.getBoolean("Visible"), s.getBoolean("Icon")));
            }
        }
    }

    public static void sendDiagnostics(ServerPlayer sp) {
        List<TTPackets.DiagnosticView> out = new ArrayList<>();
        CompoundTag root = data(sp);
        if (!root.contains(APPLIED)) {
            TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new TTPackets.SyncDiagnosticsPacket(out));
            return;
        }

        ListTag applied = root.getList(APPLIED, Tag.TAG_COMPOUND);
        for (int i = 0; i < applied.size(); i++) {
            CompoundTag tag = applied.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(ID));
            if (id == null) continue;
            Tradeoff.ModifierType type;
            try { type = Tradeoff.ModifierType.valueOf(tag.getString(TYPE)); }
            catch (IllegalArgumentException ex) { continue; }

            String name = id.toString();
            String expected = "";
            String actual = "";
            boolean active = false;

            if (type == Tradeoff.ModifierType.MOB_EFFECT) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect != null) {
                    name = effect.getDescriptionId();
                    int expectedLevel = tag.getInt(AMPLIFIER) + 1;
                    expected = "Уровень " + expectedLevel;
                    MobEffectInstance current = sp.getEffect(effect);
                    if (current != null) {
                        actual = "Уровень " + (current.getAmplifier() + 1) + ", "
                                + (current.isInfiniteDuration() ? "бесконечно" : current.getDuration() + " тиков");
                        active = current.getAmplifier() == tag.getInt(AMPLIFIER) && current.isInfiniteDuration();
                    } else actual = "Не найден на игроке";
                }
            } else if (type == Tradeoff.ModifierType.ATTRIBUTE) {
                Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(id);
                if (attribute != null && tag.hasUUID(UUID_KEY)) {
                    name = attribute.getDescriptionId();
                    expected = String.format(Locale.ROOT, "%+.4f (%s)", tag.getDouble(AMOUNT), tag.getString(OPERATION));
                    AttributeInstance instance = sp.getAttribute(attribute);
                    if (instance != null) {
                        AttributeModifier modifier = instance.getModifier(tag.getUUID(UUID_KEY));
                        if (modifier != null) {
                            actual = String.format(Locale.ROOT, "%+.4f (%s)", modifier.getAmount(), modifier.getOperation());
                            active = Double.compare(modifier.getAmount(), tag.getDouble(AMOUNT)) == 0
                                    && modifier.getOperation() == AttributeModifier.Operation.valueOf(tag.getString(OPERATION));
                        } else actual = "Модификатор не найден";
                    }
                }
            } else if (type == Tradeoff.ModifierType.MNS_STAT) {
                name = id.toString();
                expected = String.format(Locale.ROOT, "%+.2f (%s)", tag.getDouble(AMOUNT), tag.getString(MNS_TYPE));
                String source = tag.getString(MNS_SOURCE);
                active = !source.isEmpty() && MnsBridge.hasMod(sp, source);
                actual = active ? "Модификатор зарегистрирован в Mine & Slash" : "Модификатор не найден в Mine & Slash";
                for (var stat : com.warg.temptradeoffs.common.MnsStatCatalog.all()) {
                    if (stat.id().equals(id)) { name = stat.name(); break; }
                }
            }
            out.add(new TTPackets.DiagnosticView(type, id, name, expected, actual, active));
        }
        TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                new TTPackets.SyncDiagnosticsPacket(out));
    }

    private static CompoundTag data(ServerPlayer sp) {
        CompoundTag persistent = sp.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    private static CompoundTag serializeTradeoff(Tradeoff t) {
        CompoundTag out = new CompoundTag(); out.putString("Id", t.id()); out.putString("Title", t.titleKey());
        out.put("Positive", serializeSide(t.positive())); out.put("Negative", serializeSide(t.negative())); return out;
    }

    private static ListTag serializeSide(List<Tradeoff.ModifierSpec> list) {
        ListTag out = new ListTag();
        for (Tradeoff.ModifierSpec s : list) {
            CompoundTag t = new CompoundTag(); t.putString(TYPE, s.type().name()); t.putString(ID, s.id().toString());
            t.putInt(AMPLIFIER, s.amplifier()); t.putDouble(AMOUNT, s.amount()); t.putString(OPERATION, s.operation().name());
            t.putInt("Weight", s.weight()); t.putString(MNS_TYPE, s.mnsModType()); out.add(t);
        }
        return out;
    }

    private static Tradeoff deserializeTradeoff(CompoundTag t) {
        if (t.isEmpty()) return null;
        return new Tradeoff(t.getString("Id"), t.getString("Title"), deserializeSide(t.getList("Positive", Tag.TAG_COMPOUND)), deserializeSide(t.getList("Negative", Tag.TAG_COMPOUND)));
    }

    private static List<Tradeoff.ModifierSpec> deserializeSide(ListTag list) {
        List<Tradeoff.ModifierSpec> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i); ResourceLocation id = ResourceLocation.tryParse(t.getString(ID)); if (id == null) continue;
            result.add(new Tradeoff.ModifierSpec(
                    Tradeoff.ModifierType.valueOf(t.getString(TYPE)), id, t.getInt(AMPLIFIER),
                    t.getDouble(AMOUNT), AttributeModifier.Operation.valueOf(t.getString(OPERATION)),
                    t.getInt("Weight"), t.contains(MNS_TYPE) ? t.getString(MNS_TYPE) : "FLAT"));
        }
        return result;
    }

    private TradeoffManager() {}
}
