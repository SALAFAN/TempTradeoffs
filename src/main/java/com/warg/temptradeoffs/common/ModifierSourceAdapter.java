package com.warg.temptradeoffs.common;

import java.util.List;

/** Source-specific classification and contribution hook used by SourceCatalog. */
public interface ModifierSourceAdapter {
    /** Stable source key used in config/UI persistence. */
    String key();

    /** Whether this adapter owns the supplied modifier. */
    boolean accepts(Tradeoff.ModifierSpec spec);

    /** Display name for the source. */
    String displayName();

    /** Ordering priority in the source browser. */
    int rank();

    /** Whether this optional source enables the extended modifier pool. */
    default boolean enablesExtendedPool() { return false; }

    /** Adds source-specific non-registry modifiers. Most adapters have none. */
    default void collectAdditionalModifiers(List<Tradeoff.ModifierSpec> out, boolean positive, boolean includeUnsupportedForConfig) {}
}
