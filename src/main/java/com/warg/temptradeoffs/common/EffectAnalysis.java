package com.warg.temptradeoffs.common;

/** Result of automatic semantic/power analysis of a modifier candidate. */
public record EffectAnalysis(
        Decision decision,
        Domain domain,
        double power,
        double confidence,
        int suggestedWeight,
        String reason
) {
    public enum Decision { APPROVED, LIMITED, EXCLUDED }
    public enum Domain {
        DAMAGE, HEALTH, REGENERATION, DEFENSE, RESISTANCE, MOBILITY,
        ATTACK_SPEED, CRIT, RESOURCE, CONTROL, VISION, UTILITY, ECONOMY, UNKNOWN
    }
    public boolean usable() { return decision != Decision.EXCLUDED; }
}
