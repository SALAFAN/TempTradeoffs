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
                int weight = MnsStatCatalog.valueWeight(stat, i);
                out.add(new Tradeoff.ModifierSpec(
                        Tradeoff.ModifierType.MNS_STAT, stat.id(), 0, amount,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION,
                        weight, "FLAT"));
            }
        }
    }
}
