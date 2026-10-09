// src/main/java/com/kn8/core/combat/CombatMath.java
package com.kn8.core.combat;

import java.util.List;

/**
 * Regras do combate do jogador em Java puro (GDD secoes 5, 7 e 12), testaveis com JUnit.
 *
 * <pre>
 * dano      = base da arma * multiplicador da acao * multiplicador do combo * (1 + R / divisor)
 * sem stamina: a acao fica {@code slowdown} vezes mais lenta (duracao e tick de impacto)
 * bloqueio  : dano recebido * (1 - reducao); custo = dano bloqueado * custo por ponto
 * </pre>
 */
public final class CombatMath {

    private CombatMath() {
    }

    public static float damage(float weaponBase, float actionMultiplier, float comboMultiplier,
            double releaseMultiplier) {
        return (float) (weaponBase * actionMultiplier * comboMultiplier * releaseMultiplier);
    }

    /**
     * Proximo passo do combo de golpes leves: continua se o golpe novo comeca ate {@code window} ticks depois do fim
     * do anterior; senao volta ao primeiro. O passo da a volta na lista de multiplicadores.
     */
    public static int nextComboStep(int previousStep, long previousEndTick, long now, int window, int comboLength) {
        if (comboLength <= 0 || previousStep < 0 || now > previousEndTick + window) {
            return 0;
        }
        return (previousStep + 1) % comboLength;
    }

    /** Multiplicador do passo (lista do JSON da arma); lista vazia = 1. */
    public static float comboMultiplier(List<Float> combo, int step) {
        if (combo.isEmpty()) {
            return 1.0F;
        }
        return combo.get(Math.floorMod(step, combo.size()));
    }

    /**
     * 0.5.0-D7 (Miguel): golpes mais rapidos conforme o Release. Fator de velocidade = 1 + {@code atFull} * R / 100
     * (R entre 0 e 100); com {@code atFull} 0,5 o golpe a 100% fica 1,5 vez mais rapido.
     */
    public static double releaseSpeed(double release, double atFull) {
        return 1.0 + Math.max(0.0, atFull) * Math.max(0.0, Math.min(100.0, release)) / 100.0;
    }

    /** Ticks de uma acao acelerada pelo fator (arredonda; 0 e negativo ficam como estao, o resto nunca abaixo de 1). */
    public static int faster(int ticks, double speed) {
        return ticks <= 0 ? ticks : Math.max(1, (int) Math.round(ticks / Math.max(1.0, speed)));
    }

    /** Duracao ou tick de impacto com a lentidao de "sem stamina" (arredonda para cima). */
    public static int slowed(int ticks, boolean hasStamina, double slowdown) {
        return hasStamina ? ticks : (int) Math.ceil(ticks * slowdown);
    }

    /** Resultado de um golpe bloqueado. */
    public record Block(float damageTaken, float staminaCost) {
    }

    public static Block block(float incoming, double reduction, double staminaPerBlockedPoint) {
        float blocked = (float) (incoming * reduction);
        return new Block(incoming - blocked, (float) (blocked * staminaPerBlockedPoint));
    }

    /**
     * Parry (GDD secao 7): o golpe chega ate {@code window} ticks depois de o bloqueio comecar. A janela cresce a
     * partir de uma % liberada (GDD secao 6: 60-69% "parry com janela maior").
     */
    public static boolean isParry(long blockStartTick, long hitTick, int release, int baseWindow, int highWindow,
            int highWindowRelease) {
        int window = release >= highWindowRelease ? highWindow : baseWindow;
        long elapsed = hitTick - blockStartTick;
        return elapsed >= 0 && elapsed <= window;
    }

    /** Dano do golpe com o critico pos-parry (consumido no primeiro golpe que acertar). */
    public static float withCritical(float damage, boolean critical, double criticalMultiplier) {
        return critical ? (float) (damage * criticalMultiplier) : damage;
    }

    /**
     * Carga do ataque carregado (GDD secao 7), de 0 a 1: abaixo de {@code minTicks} segurando e golpe pesado comum
     * (devolve -1); depois cresce linear ate {@code maxTicks} (1 = carga completa).
     */
    public static float chargeFraction(long heldTicks, int minTicks, int maxTicks) {
        if (heldTicks < minTicks) {
            return -1.0F;
        }
        if (maxTicks <= minTicks) {
            return 1.0F;
        }
        return Math.min(1.0F, (heldTicks - minTicks) / (float) (maxTicks - minTicks));
    }

    /** Multiplicador extra do ataque carregado: de 1 (carga minima) ate {@code fullMultiplier} (carga completa). */
    public static float chargedMultiplier(float fraction, double fullMultiplier) {
        return (float) (1.0 + (fullMultiplier - 1.0) * Math.max(0.0F, Math.min(1.0F, fraction)));
    }

    /** O atacante esta na frente de quem bloqueia? (produto escalar no plano horizontal > 0). */
    public static boolean inFront(double lookX, double lookZ, double toAttackerX, double toAttackerZ) {
        return lookX * toAttackerX + lookZ * toAttackerZ > 0;
    }
}
