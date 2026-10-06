// src/main/java/com/kn8/core/kaiju/KaijuBrain.java
package com.kn8.core.kaiju;

/**
 * Maquina de estados do kaiju em Java puro: recebe o que o servidor observou neste tick e devolve o estado. A IA de
 * movimento continua nos goals do Minecraft; este estado e o que as animacoes e as habilidades (M8, M10) consultam.
 *
 * <p>Prioridade: morto &gt; atordoado &gt; atacando (alvo ao alcance) &gt; perseguindo &gt; andando &gt; parado.</p>
 */
public final class KaijuBrain {

    /** Observacoes de um tick. Distancias ao quadrado para evitar raiz quadrada. */
    public record Inputs(boolean dead, int staggerTicks, boolean hasTarget, double targetDistanceSq,
            double attackReachSq, boolean moving) {
    }

    private KaijuBrain() {
    }

    public static KaijuState next(Inputs inputs) {
        if (inputs.dead()) {
            return KaijuState.DEAD;
        }
        if (inputs.staggerTicks() > 0) {
            return KaijuState.STAGGER;
        }
        if (inputs.hasTarget()) {
            return inputs.targetDistanceSq() <= inputs.attackReachSq() ? KaijuState.ATTACK : KaijuState.CHASE;
        }
        return inputs.moving() ? KaijuState.WANDER : KaijuState.IDLE;
    }
}
