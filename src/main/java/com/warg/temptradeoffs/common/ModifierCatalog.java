package com.warg.temptradeoffs.common;

import java.util.*;

/**
 * Shared classification/grouping rules for the configuration browser.
 * The UI should not need to know how a modifier is represented internally.
 */
public final class ModifierCatalog {
    private ModifierCatalog() {}

    public static boolean isEffect(Tradeoff.ModifierSpec spec) {
        return spec.type() == Tradeoff.ModifierType.MOB_EFFECT;
    }

    public static boolean isModifier(Tradeoff.ModifierSpec spec) {
        return spec.type() != Tradeoff.ModifierType.MOB_EFFECT;
    }

    public static String sourceKey(Tradeoff.ModifierSpec spec) {
        return SourceCatalog.keyOf(spec);
    }

    public static List<Tradeoff.ModifierSpec> bySource(List<Tradeoff.ModifierSpec> specs, String source) {
        List<Tradeoff.ModifierSpec> result = new ArrayList<>();
        for (Tradeoff.ModifierSpec spec : specs) {
            if (source.equals(sourceKey(spec))) result.add(spec);
        }
        return result;
    }

    public static List<Tradeoff.ModifierSpec> byType(List<Tradeoff.ModifierSpec> specs,
                                                      Tradeoff.ModifierType type) {
        List<Tradeoff.ModifierSpec> result = new ArrayList<>();
        for (Tradeoff.ModifierSpec spec : specs) {
            if (spec.type() == type) result.add(spec);
        }
        return result;
    }

    /** Stable family key for numeric attributes and Mine & Slash stats. */
    public static String modifierFamilyKey(Tradeoff.ModifierSpec spec) {
        return "other:" + spec.type().name() + ":" + spec.id();
    }

    public static Map<String, List<Tradeoff.ModifierSpec>> groupModifiers(List<Tradeoff.ModifierSpec> specs) {
        Map<String, List<Tradeoff.ModifierSpec>> groups = new LinkedHashMap<>();
        for (Tradeoff.ModifierSpec spec : specs) {
            groups.computeIfAbsent(modifierFamilyKey(spec), k -> new ArrayList<>()).add(spec);
        }
        for (List<Tradeoff.ModifierSpec> variants : groups.values()) {
            variants.sort(Comparator.comparingDouble(Tradeoff.ModifierSpec::amount));
        }
        return groups;
    }
}
