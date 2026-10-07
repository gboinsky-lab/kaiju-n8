// src/main/java/com/kn8/core/kaiju/AbilitySelection.java
package com.kn8.core.kaiju;

import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Regras puras de escolha de habilidade de kaiju (0.6), testaveis com JUnit: faixa de distancia, setor da varredura
 * e prioridade com sorteio no empate.
 */
public final class AbilitySelection {

    private AbilitySelection() {
    }

    /** Distancia entre as bordas dentro de [min, max]? */
    public static boolean inRange(double edge, double min, double max) {
        return edge >= min && edge <= max;
    }

    /**
     * O angulo (graus, relativo a frente do corpo, -180..180) esta no setor de largura {@code width} centrado em
     * {@code center}? Setor de 360 graus aceita tudo.
     */
    public static boolean inArc(double angle, double center, double width) {
        if (width >= 360.0) {
            return true;
        }
        double delta = ((angle - center) % 360.0 + 540.0) % 360.0 - 180.0;
        return Math.abs(delta) <= width / 2.0;
    }

    /**
     * Velocidade horizontal inicial para andar {@code distance} em {@code ticks} no ar, com o freio vanilla do ar
     * ({@code drag} por tick, 0,91): soma da serie geometrica.
     */
    public static double leapHorizontalSpeed(double distance, int ticks, double drag) {
        double sum = drag >= 1.0 ? ticks : (1.0 - Math.pow(drag, ticks)) / (1.0 - drag);
        return distance / sum;
    }

    /**
     * Velocidade vertical inicial para estar {@code height} acima do ponto de partida depois de {@code ticks} (salto
     * que cai no alvo), com a fisica vanilla: a cada tick sobe {@code v}, depois {@code v = (v - gravity) * drag}.
     * Busca binaria na simulacao (roda uma vez por salto).
     */
    public static double leapVerticalSpeed(double height, int ticks, double gravity, double drag) {
        double low = -2.0;
        double high = 6.0;
        for (int i = 0; i < 40; i++) {
            double mid = (low + high) / 2.0;
            if (heightAfter(mid, ticks, gravity, drag) < height) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2.0;
    }

    private static double heightAfter(double speed, int ticks, double gravity, double drag) {
        double y = 0.0;
        double v = speed;
        for (int t = 0; t < ticks; t++) {
            y += v;
            v = (v - gravity) * drag;
        }
        return y;
    }

    /**
     * Indice da habilidade escolhida: a maior prioridade; empate sorteado com {@code random} (recebe o tamanho e
     * devolve 0..tamanho-1). -1 se a lista estiver vazia.
     */
    public static int pick(List<Integer> priorities, IntUnaryOperator random) {
        if (priorities.isEmpty()) {
            return -1;
        }
        int best = Integer.MIN_VALUE;
        for (int priority : priorities) {
            best = Math.max(best, priority);
        }
        int ties = 0;
        for (int priority : priorities) {
            if (priority == best) {
                ties++;
            }
        }
        int chosen = ties > 1 ? random.applyAsInt(ties) : 0;
        for (int i = 0; i < priorities.size(); i++) {
            if (priorities.get(i) == best && chosen-- == 0) {
                return i;
            }
        }
        return -1;
    }
}
