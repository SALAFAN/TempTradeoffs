package com.warg.temptradeoffs.common;

import net.minecraftforge.fml.ModList;

import java.util.*;

/**
 * Central registry of modifier sources used by the configuration browser.
 * Sources are discovered from modifier ResourceLocation namespaces, so new
 * mods do not require a hard-coded entry in the UI.
 */
public final class SourceCatalog {
    public static final String VANILLA = "minecraft";
    public static final String MINE_AND_SLASH = "mmorpg";

    private SourceCatalog() {}

    public record Source(String key, String displayName, int rank, int count) {}

    public static String keyOf(Tradeoff.ModifierSpec spec) {
        return spec.id().getNamespace().toLowerCase(Locale.ROOT);
    }

    public static String displayName(String key) {
        if (VANILLA.equals(key)) return "Minecraft";
        if (MINE_AND_SLASH.equals(key)) return "Mine & Slash";
        try {
            return ModList.get().getModContainerById(key)
                    .map(c -> c.getModInfo().getDisplayName())
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(key);
        } catch (Throwable ignored) {
            return key;
        }
    }

    public static int rank(String key) {
        if (VANILLA.equals(key)) return 0;
        if (MINE_AND_SLASH.equals(key)) return 1;
        return 2;
    }

    public static List<Source> discover(List<Tradeoff.ModifierSpec> positive,
                                         List<Tradeoff.ModifierSpec> negative) {
        Map<String, Integer> counts = new HashMap<>();
        count(counts, positive);
        count(counts, negative);

        List<Source> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            result.add(new Source(entry.getKey(), displayName(entry.getKey()),
                    rank(entry.getKey()), entry.getValue()));
        }
        result.sort(Comparator.comparingInt(Source::rank)
                .thenComparing(Source::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Source::key));
        return List.copyOf(result);
    }

    private static void count(Map<String, Integer> counts, List<Tradeoff.ModifierSpec> specs) {
        for (Tradeoff.ModifierSpec spec : specs) {
            counts.merge(keyOf(spec), 1, Integer::sum);
        }
    }
}
