// src/main/java/com/kn8/core/power/PowerParams.java
package com.kn8.core.power;

/**
 * Parametros das regras de poder do jogador (GDD secoes 5 a 8), lidos do {@code kn8-server.toml}. Java puro para
 * as regras serem testadas com JUnit; os valores padrao do GDD ficam so na definicao do config.
 *
 * <p>0.5.0 (Biblioteca v21, decisao do Miguel): o jogador sobe/baixa o Release numa tecla
 * ({@code releaseRaisePerSecond}/{@code releaseLowerPerSecond}); dentro do limite pessoal o uso aquece o traje ate
 * o estagio WARM ({@code useHeatPerSecond}, ~1 minuto no limite); acima dele cada 10 pontos de excesso geram
 * {@code excessHeatPer10PerSecond}.</p>
 */
public record PowerParams(
        double releaseDamageDivisor,
        double speedPerRelease,
        double damageReductionPerRelease,
        double maxDamageReduction,
        double knockbackPerRelease,
        double releaseRaisePerSecond,
        double releaseLowerPerSecond,
        double staminaBase,
        double staminaPerRelease,
        double staminaRegenPerSecond,
        int staminaRegenDelayTicks,
        double warmRegenFactor,
        int heatWarmAt,
        int heatOverloadAt,
        int heatCriticalAt,
        int heatMax,
        double excessHeatPer10PerSecond,
        double useHeatPerSecond,
        double coolOutOfCombatPerSecond,
        double coolInCombatPerSecond,
        double energyMax,
        double energyRegenPerSecond,
        int trainingXpBase,
        int trainingXpPerPoint) {

    public static final double TICKS_PER_SECOND = 20.0;
}
