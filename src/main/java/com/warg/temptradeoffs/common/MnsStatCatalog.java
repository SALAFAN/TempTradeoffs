package com.warg.temptradeoffs.common;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Reflection-only adapter for Mine and Slash 1.20.1.
 *
 * This deliberately has no compile-time dependency on Mine and Slash, so the
 * integration remains optional. The class names below were inspected from
 * Mine_and_Slash-1.20.1-6.4.13.jar.
 */
public final class MnsStatCatalog {
    private static final String MOD_ID = "mmorpg";
    private static final String STAT_BASE = "com.robertx22.mine_and_slash.database.data.stats.Stat";
    private static final String STAT_PACKAGE = "com.robertx22.mine_and_slash.database.data.stats.types";

    private static final String STATS_REGISTER =
            "com.robertx22.mine_and_slash.database.registrators.StatsRegister";
    private static final String GENERATED =
            "com.robertx22.mine_and_slash.uncommon.interfaces.IGenerated";
    private static final String REGEN_PERCENT =
            "com.robertx22.mine_and_slash.database.data.stats.types.resources.RegeneratePercentStat";

    private static volatile List<Descriptor> cache;

    public record Descriptor(ResourceLocation id, String name, String description, boolean percent) {
        public String guid() { return id.getPath(); }
    }

    public static boolean isAvailable() {
        return TTConfig.ENABLE_CTE2_INTEGRATION.get() && ModList.get().isLoaded(MOD_ID);
    }

    public static List<Descriptor> all() {
        List<Descriptor> ready = cache;
        if (ready != null) return ready;
        synchronized (MnsStatCatalog.class) {
            if (cache != null) return cache;

            List<Descriptor> result = new ArrayList<>();
            if (!isAvailable()) {
                cache = List.of();
                return cache;
            }

            Set<String> seen = new HashSet<>();
            try {
                ClassLoader cl = MnsStatCatalog.class.getClassLoader();
                Class<?> registerClass = Class.forName(STATS_REGISTER, true, cl);
                Object register = registerClass.getConstructor().newInstance();

                // StatsRegister$1 is the exact base-stat list used by MnS's own
                // registration code. It is intentionally used here instead of
                // guessing GUIDs or scanning arbitrary classes.
                Class<?> listClass = Class.forName(STATS_REGISTER + "$1", true, cl);
                var ctor = listClass.getDeclaredConstructor(registerClass);
                ctor.setAccessible(true);
                Object list = ctor.newInstance(register);

                if (list instanceof Iterable<?> iterable) {
                    for (Object stat : iterable) {
                        addDescriptor(result, seen, stat);
                        addGeneratedVariants(result, seen, stat);
                    }
                }
            } catch (Throwable ignored) {
                // Fall back to the few resource stats that are especially
                // important to CTE2 if StatsRegister cannot be reflected.
                addClass(result, seen, STAT_PACKAGE + ".spirit.AuraCapacity");
                addClass(result, seen, STAT_PACKAGE + ".spirit.AuraEffect");
                addClass(result, seen, STAT_PACKAGE + ".resources.mana.Mana");
                addClass(result, seen, STAT_PACKAGE + ".resources.energy.Energy");
                addClass(result, seen, STAT_PACKAGE + ".resources.health.Health");
            }

            addStaticStatFields(result, seen, REGEN_PERCENT);
            result.sort(Comparator.comparing(d -> d.id().toString()));
            cache = Collections.unmodifiableList(result);
            return cache;
        }
    }

    private static void addGeneratedVariants(List<Descriptor> result, Set<String> seen, Object stat) {
        try {
            Class<?> generated = Class.forName(GENERATED, false, stat.getClass().getClassLoader());
            if (!generated.isInstance(stat)) return;
            Method method = stat.getClass().getMethod("generateAllPossibleStatVariations");
            Object variations = method.invoke(stat);
            if (variations instanceof Iterable<?> iterable) {
                for (Object variant : iterable) addDescriptor(result, seen, variant);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addClass(List<Descriptor> result, Set<String> seen, String className) {
        try {
            Class<?> clazz = Class.forName(className, true, MnsStatCatalog.class.getClassLoader());
            Class<?> statBase = Class.forName(STAT_BASE, false, clazz.getClassLoader());
            if (!statBase.isAssignableFrom(clazz)) return;

            Method getter = clazz.getMethod("getInstance");
            Object stat = getter.invoke(null);
            addDescriptor(result, seen, stat);
        } catch (Throwable ignored) {
            // Some versions of MnS remove/rename a stat. Optional integration
            // must simply skip it rather than preventing Minecraft from loading.
        }
    }

    private static void addStaticStatFields(List<Descriptor> result, Set<String> seen, String className) {
        try {
            Class<?> clazz = Class.forName(className, true, MnsStatCatalog.class.getClassLoader());
            for (Field field : clazz.getFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                Object stat = field.get(null);
                if (stat != null) addDescriptor(result, seen, stat);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addDescriptor(List<Descriptor> result, Set<String> seen, Object stat) {
        try {
            Method guidMethod = stat.getClass().getMethod("GUID");
            String guid = String.valueOf(guidMethod.invoke(stat));
            if (guid.isBlank() || !seen.add(guid)) return;

            boolean percent = Boolean.TRUE.equals(stat.getClass().getMethod("IsPercent").invoke(stat));
            String name = String.valueOf(stat.getClass().getMethod("locNameForLangFile").invoke(stat));
            String description = String.valueOf(stat.getClass().getMethod("locDescForLangFile").invoke(stat));
            result.add(new Descriptor(new ResourceLocation(MOD_ID, guid), name, description, percent));
        } catch (Throwable ignored) {
        }
    }

    public static int weightFor(Descriptor d) {
        String p = d.guid().toLowerCase(Locale.ROOT);
        if (p.contains("loot") || p.contains("find") || p.contains("chance") || p.contains("quantity")
                || p.contains("rarity") || p.contains("pack_size")) return 320;
        if (p.contains("damage") || p.contains("armor") || p.contains("resist")
                || p.contains("dodge") || p.contains("block")) return 240;
        if (p.contains("health") || p.contains("mana") || p.contains("energy")
                || p.contains("magic_shield") || p.contains("blood")) return 180;
        if (p.contains("spirit_cost") || p.contains("aura_effect")) return 210;
        return 220;
    }

    public static double[] values(Descriptor d) {
        String p = d.guid().toLowerCase(Locale.ROOT);
        if (p.equals("spirit_cost")) return new double[]{1, 2, 3, 5, 8};
        if (p.equals("health") || p.equals("mana") || p.equals("energy") || p.equals("blood")) {
            return new double[]{5, 10, 20, 35, 50};
        }
        if (p.contains("regen")) return new double[]{1, 2, 5, 10, 20};
        if (d.percent()) return new double[]{2, 5, 10, 15, 25};
        if (p.contains("damage") || p.contains("armor") || p.contains("dodge")
                || p.contains("block") || p.contains("penetration")) {
            return new double[]{2, 5, 10, 15, 25};
        }
        return new double[]{1, 2, 5, 10, 20};
    }

    public static int valueWeight(Descriptor d, int index) {
        int base = weightFor(d);
        return Math.min(950, base + index * Math.max(20, base / 2));
    }

    private MnsStatCatalog() {}
}
