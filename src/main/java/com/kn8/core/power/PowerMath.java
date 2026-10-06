// src/main/java/com/kn8/core/power/PowerMath.java
package com.kn8.core.power;

/**
 * Formulas dos atributos do jogador (GDD secoes 5 a 8), em Java puro. Tudo por tick do servidor.
 *
 * <pre>
 * dano          = base * (1 + R / divisor)
 * velocidade    = +speedPerRelease * R
 * reducao       = min(damageReductionPerRelease * R, maxDamageReduction)
 * knockback     = knockbackPerRelease * R
 * stamina max   = staminaBase + staminaPerRelease * R
 * </pre>
 */
public final class PowerMath {

    private PowerMath() {
    }

    public static double damageMultiplier(double release, PowerParams params) {
        return 1.0 + release / params.releaseDamageDivisor();
    }

    public static double speedBonus(double release, PowerParams params) {
        return params.speedPerRelease() * release;
    }

    public static double damageReduction(double release, PowerParams params) {
        return Math.min(params.damageReductionPerRelease() * release, params.maxDamageReduction());
    }

    public static double knockbackResistance(double release, PowerParams params) {
        return params.knockbackPerRelease() * release;
    }

    public static double maxStamina(double release, PowerParams params) {
        return params.staminaBase() + params.staminaPerRelease() * release;
    }

    /**
     * Stamina depois de um tick: so regenera depois de {@code staminaRegenDelayTicks} sem gastar; regenera mais
     * devagar a partir do estagio WARM de calor.
     */
    public static double staminaAfterTick(double stamina, double max, long ticksSinceSpent, HeatStage stage,
            PowerParams params) {
        if (ticksSinceSpent < params.staminaRegenDelayTicks()) {
            return Math.min(stamina, max);
        }
        double factor = stage == HeatStage.NORMAL ? 1.0 : params.warmRegenFactor();
        double regen = params.staminaRegenPerSecond() / PowerParams.TICKS_PER_SECOND * factor;
        return Math.min(max, stamina + regen);
    }

    /** Estagio de calor; PANIC exige chegar ao maximo. */
    public static HeatStage heatStage(double heat, PowerParams params) {
        if (heat >= params.heatMax()) {
            return HeatStage.PANIC;
        }
        if (heat >= params.heatCriticalAt()) {
            return HeatStage.CRITICAL;
        }
        if (heat >= params.heatOverloadAt()) {
            return HeatStage.OVERLOAD;
        }
        if (heat >= params.heatWarmAt()) {
            return HeatStage.WARM;
        }
        return HeatStage.NORMAL;
    }

    /**
     * Calor depois de um tick: o Surto gera {@code surgeHeatPer10PerSecond} a cada 10 pontos acima do treinado;
     * sem Surto o traje esfria (mais rapido fora de combate). Resultado entre 0 e o maximo.
     */
    public static double heatAfterTick(double heat, int surge, boolean inCombat, PowerParams params) {
        double next;
        if (surge > 0) {
            next = heat + params.surgeHeatPer10PerSecond() * (surge / 10.0) / PowerParams.TICKS_PER_SECOND;
        } else {
            double cooling = inCombat ? params.coolInCombatPerSecond() : params.coolOutOfCombatPerSecond();
            next = heat - cooling / PowerParams.TICKS_PER_SECOND;
        }
        return Math.max(0.0, Math.min(params.heatMax(), next));
    }

    public static double energyAfterTick(double energy, PowerParams params) {
        return Math.min(params.energyMax(), energy + params.energyRegenPerSecond() / PowerParams.TICKS_PER_SECOND);
    }

    /** XP de treinamento para subir do ponto {@code current} para o seguinte (cada ponto custa mais). */
    public static int xpForNextPoint(int current, PowerParams params) {
        return params.trainingXpBase() + params.trainingXpPerPoint() * current;
    }

    /** Resultado de converter XP de treino em pontos. */
    public record Training(int trained, int xp) {
    }

    /**
     * Converte XP em pontos ate o teto. No teto, o XP guardado fica limitado ao custo de UM ponto (reserva): ao subir
     * o teto o jogador ganha no maximo +1 ponto imediato e o resto precisa ser treinado (decisao do M6).
     */
    public static Training convertTraining(int trained, int xp, int cap, PowerParams params) {
        int points = trained;
        int left = Math.max(0, xp);
        while (points < cap && left >= xpForNextPoint(points, params)) {
            left -= xpForNextPoint(points, params);
            points++;
        }
        if (points >= cap) {
            left = Math.min(left, xpForNextPoint(points, params));
        }
        return new Training(points, left);
    }

    /** % efetiva: treinado limitado pelo teto, mais o Surto (limitado), sempre entre 0 e 100. */
    public static int effectiveRelease(int trained, int cap, int surge, PowerParams params) {
        int base = Math.min(trained, cap);
        int boost = Math.max(0, Math.min(surge, params.surgeMax()));
        return Math.max(0, Math.min(100, base + boost));
    }
}
