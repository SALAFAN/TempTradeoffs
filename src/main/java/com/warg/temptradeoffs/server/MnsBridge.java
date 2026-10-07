package com.warg.temptradeoffs.server;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * Runtime bridge to Mine and Slash 6.4.x.
 *
 * No compile-time MnS dependency is used. The bridge talks to the exact
 * CustomExactStatsData API present in Mine_and_Slash-1.20.1-6.4.13.jar:
 *
 * Load.Unit(entity) -> EntityData -> getCustomExactStats()
 * CustomExactStatsData.addMod(stat, source, min, max, ModType)
 * CustomExactStatsData.removeMod(source)
 */
public final class MnsBridge {
    private static final String LOAD = "com.robertx22.mine_and_slash.uncommon.datasaving.Load";
    private static final String MOD_TYPE = "com.robertx22.mine_and_slash.uncommon.enumclasses.ModType";

    private static Method unitMethod;
    private static Method customStatsMethod;
    private static Method addModMethod;
    private static Method removeModMethod;
    private static Object flatType;
    private static volatile boolean initialized;

    private static void init() {
        if (initialized) return;
        synchronized (MnsBridge.class) {
            if (initialized) return;
            try {
                ClassLoader cl = MnsBridge.class.getClassLoader();
                Class<?> load = Class.forName(LOAD, false, cl);
                Class<?> entity = Class.forName("net.minecraft.world.entity.Entity", false, cl);
                Class<?> entityData = Class.forName(
                        "com.robertx22.mine_and_slash.capability.entity.EntityData", false, cl);
                Class<?> customStats = Class.forName(
                        "com.robertx22.mine_and_slash.saveclasses.CustomExactStatsData", false, cl);
                Class<?> modType = Class.forName(MOD_TYPE, false, cl);

                unitMethod = load.getMethod("Unit", entity);
                customStatsMethod = entityData.getMethod("getCustomExactStats");
                addModMethod = customStats.getMethod(
                        "addMod", String.class, String.class, float.class, float.class, modType);
                removeModMethod = customStats.getMethod("removeMod", String.class);
                flatType = Enum.valueOf((Class<? extends Enum>) modType.asSubclass(Enum.class), "FLAT");
            } catch (Throwable ignored) {
                unitMethod = null;
            } finally {
                initialized = true;
            }
        }
    }

    public static boolean available() {
        init();
        return unitMethod != null;
    }

    public static boolean add(ServerPlayer player, String statGuid, String sourceId, float value, String modTypeName) {
        init();
        if (unitMethod == null) return false;
        try {
            Object entityData = unitMethod.invoke(null, player);
            Object customStats = customStatsMethod.invoke(entityData);
            Object modType = enumValue(modTypeName);
            addModMethod.invoke(customStats, statGuid, sourceId, value, value, modType);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasMod(ServerPlayer player, String sourceId) {
        init();
        if (unitMethod == null) return false;
        try {
            Object entityData = unitMethod.invoke(null, player);
            Object customStats = customStatsMethod.invoke(entityData);
            java.lang.reflect.Field mods = customStats.getClass().getField("mods");
            Object value = mods.get(customStats);
            return value instanceof java.util.Map<?, ?> map && map.containsKey(sourceId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean remove(ServerPlayer player, String sourceId) {
        init();
        if (removeModMethod == null) return false;
        try {
            Object entityData = unitMethod.invoke(null, player);
            Object customStats = customStatsMethod.invoke(entityData);
            removeModMethod.invoke(customStats, sourceId);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object enumValue(String name) {
        try {
            Class<?> enumClass = flatType.getClass();
            return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
        } catch (Throwable ignored) {
            return flatType;
        }
    }

    private MnsBridge() {}
}
