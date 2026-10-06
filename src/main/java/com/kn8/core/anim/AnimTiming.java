// src/main/java/com/kn8/core/anim/AnimTiming.java
package com.kn8.core.anim;

/**
 * Sincronia de animacao disparada pelo servidor (Java puro). Criterio "Animacao" da Fase 4: a animacao tem de bater
 * com o tick do servidor em ate 2 ticks em todos os clientes.
 *
 * <p>O pacote leva o tick do servidor em que a acao comecou. Quem recebe atrasado (latencia, ou um cliente que
 * comecou a ver o jogador no meio da acao) inicia a animacao ja adiantada pelo atraso, ate um limite: atraso maior
 * que o limite toca do ponto do limite, para nao pular o golpe inteiro.</p>
 */
public final class AnimTiming {

    private AnimTiming() {
    }

    /** Ticks que a animacao deve pular ao comecar: atraso medido, entre 0 e {@code maxCatchUpTicks}. */
    public static int catchUpTicks(long serverTick, long clientTick, int maxCatchUpTicks) {
        long delay = clientTick - serverTick;
        if (delay <= 0) {
            return 0;
        }
        return (int) Math.min(delay, Math.max(0, maxCatchUpTicks));
    }

    /** Atraso bruto em ticks (negativo = relogio do cliente adiantado), para diagnostico. */
    public static long delayTicks(long serverTick, long clientTick) {
        return clientTick - serverTick;
    }
}
