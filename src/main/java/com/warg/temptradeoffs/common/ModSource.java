package com.warg.temptradeoffs.common;

import net.minecraftforge.fml.ModList;

/** Generic adapter for every mod namespace not claimed by a specific source. */
public final class ModSource implements ModifierSourceAdapter {
    public static final String KEY = "__mod__";

    @Override public String key() { return KEY; }
    @Override public boolean accepts(Tradeoff.ModifierSpec spec) {
        if (spec == null) return false;
        String namespace = spec.id().getNamespace();
        return !VanillaSource.KEY.equals(namespace) && !MineAndSlashSource.KEY.equals(namespace);
    }
    /** For a generic mod, the actual namespace is the source key. */
    public String sourceKey(Tradeoff.ModifierSpec spec) {
        return spec.id().getNamespace();
    }
    public String displayName(String sourceKey) {
        try {
            return ModList.get().getModContainerById(sourceKey)
                    .map(c -> c.getModInfo().getDisplayName())
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(sourceKey);
        } catch (Throwable ignored) {
            return sourceKey;
        }
    }
    @Override public String displayName() { return "Mod"; }
    @Override public int rank() { return 2; }
}
