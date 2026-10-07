package com.warg.temptradeoffs.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TradeoffPool {
    private static final String[] TITLES = {
            "choice.temptradeoffs.fate",
            "choice.temptradeoffs.risk",
            "choice.temptradeoffs.bargain",
            "choice.temptradeoffs.temptation",
            "choice.temptradeoffs.exchange",
            "choice.temptradeoffs.gamble"
    };

    public static List<Tradeoff> randomChoices(Random random, int count) {
        List<Tradeoff> result = new ArrayList<>();
        int target = Math.max(2, Math.min(3, count));

        for (int i = 0; i < target; i++) {
            result.add(new Tradeoff(
                    "generated_" + random.nextLong() + "_" + i,
                    TITLES[random.nextInt(TITLES.length)],
                    EffectCatalog.positive(random),
                    EffectCatalog.negative(random)
            ));
        }

        return result;
    }

    private TradeoffPool() {}
}
