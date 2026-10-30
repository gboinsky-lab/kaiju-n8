// src/main/java/com/kn8/core/combat/FreezeStages.java
package com.kn8.core.combat;

/**
 * COMBATE-7 (obra: o gelo do Reno progride pelo corpo do kaiju e bloqueia o movimento em etapas): contas puras do
 * gelo em etapas (JUnit). A Lentidao sobe a cada etapa ate a do tiro e o gelo anda do ponto atingido ao centro do
 * corpo.
 */
public final class FreezeStages {

    private FreezeStages() {
    }

    /** Quantas etapas o tiro usa: nunca mais que o nivel da Lentidao (cada etapa sobe pelo menos um nivel). */
    public static int stages(int configured, int slowLevel) {
        return Math.max(1, Math.min(configured, slowLevel));
    }

    /** Nivel da Lentidao (1 = Lentidao I) na etapa {@code stage} (0 = a primeira) de {@code stages}. */
    public static int level(int stage, int stages, int slowLevel) {
        if (stages <= 1) {
            return slowLevel;
        }
        int clamped = Math.max(0, Math.min(stage, stages - 1));
        return Math.max(1, (int) Math.ceil(slowLevel * (clamped + 1) / (double) stages));
    }

    /** Fracao do caminho do ponto atingido ao centro do corpo em que o gelo esta na etapa (0 no tiro, 1 no fim). */
    public static double reach(int stage, int stages) {
        return stages <= 1 ? 1.0 : Math.max(0.0, Math.min(1.0, stage / (double) (stages - 1)));
    }

    /** Duracao da Lentidao da etapa: o gelo termina junto, ticks depois do tiro, qualquer que seja a etapa. */
    public static int duration(int stage, int stageTicks, int slowTicks) {
        return Math.max(1, slowTicks - stage * stageTicks);
    }
}
