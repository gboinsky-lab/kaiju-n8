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
 *
 * <p>0.5.0 (Biblioteca v21): R e a % ATIVA que o jogador liberou na tecla (so com traje), nao mais o treinado. O
 * treinado virou o limite pessoal: passar dele aquece o traje e desgasta o corpo, sem baixar a %.</p>
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

    /**
     * Corrida (GDD secao 7): enquanto corre, gasta {@code costPerTick} de stamina. Quem zera fica "sem folego" e so
     * volta a correr ao recuperar {@code minToSprint} (histerese: sem ela o jogador piscaria entre correr e andar).
     */
    public static boolean windedAfterTick(boolean winded, double stamina, double costPerTick, double minToSprint) {
        if (winded) {
            return stamina < minToSprint;
        }
        return costPerTick > 0 && stamina < costPerTick;
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
     * 0.5.0: % ativa depois de um tick com a tecla de Release: {@code direction} +1 sobe, -1 desce, 0 mantem.
     * Fica entre 0 e 100 e pode passar do limite pessoal (o excesso gera calor e desgasta o corpo).
     */
    public static double releaseAfterInput(double active, int direction, PowerParams params) {
        double step = direction > 0 ? params.releaseRaisePerSecond() : direction < 0
                ? -params.releaseLowerPerSecond() : 0.0;
        return Math.max(0.0, Math.min(100.0, active + step / PowerParams.TICKS_PER_SECOND));
    }

    /** Pontos acima do limite pessoal (0 dentro dele). */
    public static int excess(int active, int limit) {
        return Math.max(0, active - Math.max(0, limit));
    }

    /**
     * Calor depois de um tick (0.5.0, decisao do Miguel). Acima do limite pessoal cada 10 pontos de excesso geram
     * {@code excessHeatPer10PerSecond}, ate o maximo. Dentro do limite o uso aquece {@code useHeatPerSecond} vezes a
     * fracao usada do limite, mas so ate o estagio WARM (cansaco sem dano; ~1 minuto no limite); o que passou disso
     * esfria ate WARM. Com o Release desligado o traje esfria (mais rapido fora de combate). Resultado entre 0 e o
     * maximo.
     */
    public static double heatAfterTick(double heat, int active, int limit, boolean inCombat, PowerParams params) {
        double cooling = (inCombat ? params.coolInCombatPerSecond() : params.coolOutOfCombatPerSecond())
                / PowerParams.TICKS_PER_SECOND;
        int over = excess(active, limit);
        double next;
        if (over > 0) {
            next = heat + params.excessHeatPer10PerSecond() * (over / 10.0) / PowerParams.TICKS_PER_SECOND;
        } else if (active > 0) {
            double used = limit > 0 ? Math.min(1.0, active / (double) limit) : 1.0;
            double cap = params.heatWarmAt();
            if (heat < cap) {
                next = Math.min(cap, heat + params.useHeatPerSecond() * used / PowerParams.TICKS_PER_SECOND);
            } else {
                next = Math.max(cap, heat - cooling);
            }
        } else {
            next = heat - cooling;
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

    /**
     * 0.5 (ideia do Miguel): pontos de % que sobem sozinhos com a vida baixa. Zero acima de {@code threshold}; cresce
     * em linha reta ate {@code maxPoints} com a vida em 0. Vale para o jogador e, depois, para os soldados especiais.
     */
    public static int desperationBonus(double healthFraction, double threshold, int maxPoints) {
        if (threshold <= 0 || maxPoints <= 0 || healthFraction >= threshold) {
            return 0;
        }
        double depth = (threshold - Math.max(0.0, healthFraction)) / threshold;
        return (int) Math.round(maxPoints * Math.min(1.0, depth));
    }

    /** 0.5.0: % efetiva = % ativa (escolhida na tecla) mais o desespero, entre 0 e 100. */
    public static int effectiveRelease(int active, int desperation) {
        return Math.max(0, Math.min(100, active + Math.max(0, desperation)));
    }
}
