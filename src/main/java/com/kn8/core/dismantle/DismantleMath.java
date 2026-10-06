// src/main/java/com/kn8/core/dismantle/DismantleMath.java
package com.kn8.core.dismantle;

/**
 * Divisao do desmonte por etapas (M11b), em Java puro: o total de cada material e sorteado uma vez (na morte do
 * kaiju) e entregue aos poucos; a soma das etapas da exatamente o total, sem perder nem duplicar itens.
 */
public final class DismantleMath {

    private DismantleMath() {
    }

    /** Quanto entregar na etapa {@code step} (1..steps) de um total. */
    public static int share(int total, int step, int steps) {
        if (steps <= 0 || step < 1 || step > steps) {
            return 0;
        }
        return total * step / steps - total * (step - 1) / steps;
    }

    /** Progresso: true quando a etapa atual fecha (ticks acumulados alcancaram o necessario). */
    public static boolean stepComplete(int progressTicks, int ticksPerStep) {
        return progressTicks >= ticksPerStep;
    }
}
