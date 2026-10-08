package com.warg.temptradeoffs.common;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraftforge.fml.ModList;

import java.util.List;

/** Adapter for Mine & Slash. The integration itself remains optional. */
public final class MineAndSlashSource implements ModifierSourceAdapter {
    public static final String KEY = "mmorpg";

    @Override public String key() { return KEY; }
    @Override public boolean accepts(Tradeoff.ModifierSpec spec) {
        return spec != null && KEY.equals(spec.id().getNamespace());
    }
    @Override public String displayName() { return "Mine & Slash"; }
    @Override public int rank() { return 1; }
    @Override public boolean enablesExtendedPool() {
        return TTConfig.ENABLE_CTE2_INTEGRATION.get()
                && ModList.get().isLoaded(KEY)
                && (ModList.get().isLoaded("exile_overlay") || ModList.get().isLoaded("library_of_exile"));
    }
    @Override public void collectAdditionalModifiers(List<Tradeoff.ModifierSpec> out, boolean positive, boolean includeUnsupportedForConfig) {
        if (!enablesExtendedPool()) return;
        for (MnsStatCatalog.Descriptor stat : MnsStatCatalog.all()) {
            if (!includeUnsupportedForConfig && !stat.eligible()) continue;
            double[] values = MnsStatCatalog.values(stat);
            for (int i = 0; i < values.length; i++) {
                double amount = positive ? values[i] : -values[i];
                Tradeoff.ModifierSpec probe = new Tradeoff.ModifierSpec(
                        Tradeoff.ModifierType.MNS_STAT, stat.id(), 0, amount,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION,
                        1, "FLAT");
                EffectAnalysis analysis = EffectAnalyzer.analyze(probe, positive);
                // The configuration browser must expose even uncertain but
                // non-dangerous stats so the player can opt into them manually.
                // The actual pool uses the strict automatic decision below.
                if (!includeUnsupportedForConfig) {
                    if (!stat.eligible() || !analysis.usable() || analysis.confidence() < 0.70) continue;
                }
                int weight = analysis.suggestedWeight();
                out.add(new Tradeoff.ModifierSpec(
                        Tradeoff.ModifierType.MNS_STAT, stat.id(), 0, amount,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION,
                        weight, "FLAT"));
            }
        }
    }
}
