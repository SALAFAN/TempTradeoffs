package com.warg.temptradeoffs.common;

import java.util.List;
import java.util.Random;

public final class TradeoffPool {
    public static final List<Tradeoff> ALL = List.of(
        Tradeoff.of("berserker", "choice.temptradeoffs.berserker", Tradeoff.e("minecraft:strength", 1), Tradeoff.e("minecraft:weakness", 0)),
        Tradeoff.of("adrenaline", "choice.temptradeoffs.adrenaline", Tradeoff.e("minecraft:speed", 1), Tradeoff.e("minecraft:hunger", 1)),
        Tradeoff.of("iron_skin", "choice.temptradeoffs.iron_skin", Tradeoff.e("minecraft:resistance", 1), Tradeoff.e("minecraft:slowness", 0)),
        Tradeoff.of("night_owl", "choice.temptradeoffs.night_owl", Tradeoff.e("minecraft:night_vision", 0), Tradeoff.e("minecraft:mining_fatigue", 0)),
        Tradeoff.of("hunter", "choice.temptradeoffs.hunter", Tradeoff.e("minecraft:haste", 1), Tradeoff.e("minecraft:weakness", 0)),
        Tradeoff.of("regenerator", "choice.temptradeoffs.regenerator", Tradeoff.e("minecraft:regeneration", 1), Tradeoff.e("minecraft:hunger", 2)),
        Tradeoff.of("feather", "choice.temptradeoffs.feather", Tradeoff.e("minecraft:jump_boost", 1), Tradeoff.e("minecraft:slow_falling", 0)),
        Tradeoff.of("water_breath", "choice.temptradeoffs.water_breath", Tradeoff.e("minecraft:water_breathing", 0), Tradeoff.e("minecraft:weakness", 1)),
        Tradeoff.of("berserk_speed", "choice.temptradeoffs.berserk_speed", Tradeoff.e("minecraft:speed", 2), Tradeoff.e("minecraft:weakness", 1)),
        Tradeoff.of("guardian", "choice.temptradeoffs.guardian", Tradeoff.e("minecraft:absorption", 2), Tradeoff.e("minecraft:slowness", 1))
    );

    public static List<Tradeoff> randomChoices(Random random, int count) {
        java.util.ArrayList<Tradeoff> copy = new java.util.ArrayList<>(ALL);
        java.util.Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }
    private TradeoffPool() {}
}
