// src/main/java/com/kn8/core/power/PowerParams.java
package com.kn8.core.power;

/**
 * Parametros das regras de poder do jogador (GDD secoes 5 a 8), lidos do {@code kn8-server.toml}. Java puro para
 * as regras serem testadas com JUnit; os valores padrao do GDD ficam so na definicao do config.
 */
public record PowerParams(
        double releaseDamageDivisor,
        double speedPerRelease,
        double damageReductionPerRelease,
        double maxDamageReduction,
        double knockbackPerRelease,
        int surgeMax,
        double staminaBase,
        double staminaPerRelease,
        double staminaRegenPerSecond,
        int staminaRegenDelayTicks,
        double warmRegenFactor,
        int heatWarmAt,
        int heatOverloadAt,
        int heatCriticalAt,
        int heatMax,
        double surgeHeatPer10PerSecond,
        double coolOutOfCombatPerSecond,
        double coolInCombatPerSecond,
        double energyMax,
        double energyRegenPerSecond,
        int trainingXpBase,
        int trainingXpPerPoint) {

    public static final double TICKS_PER_SECOND = 20.0;
}
