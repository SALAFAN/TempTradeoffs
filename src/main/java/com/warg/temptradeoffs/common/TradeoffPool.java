package com.warg.temptradeoffs.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Generates the actual risk/reward cards. The card rarity controls the base
 * budgets; unusually strong modifiers automatically increase the opposite side.
 */
public final class TradeoffPool {
    private static final String[] TITLES = {
            "choice.temptradeoffs.fate",
            "choice.temptradeoffs.risk",
            "choice.temptradeoffs.bargain",
            "choice.temptradeoffs.temptation",
            "choice.temptradeoffs.exchange",
            "choice.temptradeoffs.gamble"
    };

    /* Rarity probabilities, out of 100. Cursed/Blessed are intentionally rare. */
    private static final int COMMON_WEIGHT = 45;
    private static final int UNCOMMON_WEIGHT = 25;
    private static final int RARE_WEIGHT = 15;
    private static final int EPIC_WEIGHT = 8;
    private static final int LEGENDARY_WEIGHT = 5;
    private static final int CURSED_WEIGHT = 1;
    private static final int BLESSED_WEIGHT = 1;

    /* A manually tuned catalog uses 800/900/1000-ish weights for its strongest
     * positive effects. These thresholds deliberately align the automatic
     * compensation with those hand-balanced values. */
    private static final int STRONG_THRESHOLD = 850;
    private static final int VERY_STRONG_THRESHOLD = 900;
    private static final int EXTREME_THRESHOLD = 1000;

    private TradeoffPool() {}

    public static List<Tradeoff> randomChoices(Random random, int count) {
        List<Tradeoff> result = new ArrayList<>();
        int target = Math.max(2, Math.min(3, count));

        for (int i = 0; i < target; i++) {
            Tradeoff.Rarity rarity = randomRarity(random);
            List<Tradeoff.ModifierSpec> positive;
            List<Tradeoff.ModifierSpec> negative;

            if (rarity == Tradeoff.Rarity.CURSED) {
                negative = EffectCatalog.veryStrongNegativePair(random);
                // Cursed cards have two very strong penalties with no negative
                // budget cap. Because those penalties are deliberately severe,
                // their compensation is also allowed to enlarge the positive pool.
                int positiveBudget = oppositeBudget(rarity.positiveBudget(), maxWeight(negative));
                positive = EffectCatalog.positive(random, positiveBudget);
            } else {
                int positiveBudget = rarity.positiveBudget();
                int negativeBudget = rarity.negativeBudget();

                // Two short balancing passes are enough to let a strong modifier on
                // either side enlarge the opposite budget without making card
                // generation depend on a long feedback loop.
                positive = List.of();
                negative = List.of();
                for (int pass = 0; pass < 2; pass++) {
                    positive = EffectCatalog.positive(random, positiveBudget);
                    negativeBudget = oppositeBudget(rarity.negativeBudget(), maxWeight(positive));
                    negative = EffectCatalog.negative(random, negativeBudget);
                    positiveBudget = oppositeBudget(rarity.positiveBudget(), maxWeight(negative));
                }

                // Final positive pass applies the compensation discovered on the
                // last negative pass. A strong final positive is already reflected
                // in the preceding negative generation and therefore does not need
                // another random reroll.
                if (positiveBudget != rarity.positiveBudget()) {
                    positive = EffectCatalog.positive(random, positiveBudget);
                }
            }

            result.add(new Tradeoff(
                    "generated_" + random.nextLong() + "_" + i,
                    TITLES[random.nextInt(TITLES.length)],
                    rarity,
                    positive,
                    negative
            ));
        }

        return result;
    }

    private static Tradeoff.Rarity randomRarity(Random random) {
        int roll = random.nextInt(100);
        if ((roll -= COMMON_WEIGHT) < 0) return Tradeoff.Rarity.COMMON;
        if ((roll -= UNCOMMON_WEIGHT) < 0) return Tradeoff.Rarity.UNCOMMON;
        if ((roll -= RARE_WEIGHT) < 0) return Tradeoff.Rarity.RARE;
        if ((roll -= EPIC_WEIGHT) < 0) return Tradeoff.Rarity.EPIC;
        if ((roll -= LEGENDARY_WEIGHT) < 0) return Tradeoff.Rarity.LEGENDARY;
        if ((roll -= CURSED_WEIGHT) < 0) return Tradeoff.Rarity.CURSED;
        return Tradeoff.Rarity.BLESSED;
    }

    /**
     * Raises the opposite pool only when the selected side contains a genuinely
     * expensive modifier. The increments are deliberately tied to the hand-tuned
     * 800/900/1000 scale rather than to arbitrary percentages.
     */
    public static int oppositeBudget(int baseBudget, int strongestWeight) {
        if (baseBudget == Integer.MAX_VALUE) return baseBudget;
        if (strongestWeight >= EXTREME_THRESHOLD) return safeAdd(baseBudget, 750);
        if (strongestWeight >= VERY_STRONG_THRESHOLD) return safeAdd(baseBudget, 500);
        if (strongestWeight >= STRONG_THRESHOLD) return safeAdd(baseBudget, 250);
        return baseBudget;
    }

    private static int safeAdd(int base, int extra) {
        long result = (long) base + extra;
        return (int) Math.min(10000L, result);
    }

    private static int maxWeight(List<Tradeoff.ModifierSpec> specs) {
        int max = 0;
        for (Tradeoff.ModifierSpec spec : specs) max = Math.max(max, spec.weight());
        return max;
    }
}
