package com.warg.temptradeoffs.common;

/** Adapter for vanilla Minecraft registry entries. */
public final class VanillaSource implements ModifierSourceAdapter {
    public static final String KEY = "minecraft";

    @Override public String key() { return KEY; }
    @Override public boolean accepts(Tradeoff.ModifierSpec spec) {
        return spec != null && KEY.equals(spec.id().getNamespace());
    }
    @Override public String displayName() { return "Minecraft"; }
    @Override public int rank() { return 0; }
}
