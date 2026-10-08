package com.warg.temptradeoffs.common;

import java.util.*;

/**
 * Generic source registry for the configuration browser. Concrete integration
 * knowledge lives in source adapters; this catalog only dispatches to them.
 */
public final class SourceCatalog {
    private static final VanillaSource VANILLA = new VanillaSource();
    private static final MineAndSlashSource MINE_AND_SLASH = new MineAndSlashSource();
    private static final ModSource MOD = new ModSource();
    private static final List<ModifierSourceAdapter> ADAPTERS = List.of(VANILLA, MINE_AND_SLASH, MOD);

    private SourceCatalog() {}

    public record Source(String key, String displayName, int rank, int count) {}

    public static String keyOf(Tradeoff.ModifierSpec spec) {
        for (ModifierSourceAdapter adapter : ADAPTERS) {
            if (adapter.accepts(spec)) {
                return adapter == MOD ? MOD.sourceKey(spec) : adapter.key();
            }
        }
        throw new IllegalStateException("No modifier source adapter for " + spec.id());
    }

    public static String displayName(String key) {
        if (VANILLA.key().equals(key)) return VANILLA.displayName();
        if (MINE_AND_SLASH.key().equals(key)) return MINE_AND_SLASH.displayName();
        return MOD.displayName(key);
    }

    public static int rank(String key) {
        if (VANILLA.key().equals(key)) return VANILLA.rank();
        if (MINE_AND_SLASH.key().equals(key)) return MINE_AND_SLASH.rank();
        return MOD.rank();
    }


    /** True when at least one optional integration enables the extended pool. */
    public static boolean extendedPoolEnabled() {
        for (ModifierSourceAdapter adapter : ADAPTERS) {
            if (adapter.enablesExtendedPool()) return true;
        }
        return false;
    }

    /** Lets source adapters contribute modifiers without exposing their APIs to EffectCatalog. */
    public static void collectAdditionalModifiers(List<Tradeoff.ModifierSpec> out, boolean positive, boolean includeUnsupportedForConfig) {
        for (ModifierSourceAdapter adapter : ADAPTERS) {
            adapter.collectAdditionalModifiers(out, positive, includeUnsupportedForConfig);
        }
    }

    public static List<Source> discover(List<Tradeoff.ModifierSpec> positive,
                                        List<Tradeoff.ModifierSpec> negative) {
        Map<String, Integer> counts = new HashMap<>();
        count(counts, positive);
        count(counts, negative);

        List<Source> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String key = entry.getKey();
            result.add(new Source(key, displayName(key), rank(key), entry.getValue()));
        }
        result.sort(Comparator.comparingInt(Source::rank)
                .thenComparing(Source::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Source::key));
        return List.copyOf(result);
    }

    private static void count(Map<String, Integer> counts, List<Tradeoff.ModifierSpec> specs) {
        for (Tradeoff.ModifierSpec spec : specs) counts.merge(keyOf(spec), 1, Integer::sum);
    }
}
