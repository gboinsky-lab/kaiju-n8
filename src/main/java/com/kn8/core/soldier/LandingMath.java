// src/main/java/com/kn8/core/soldier/LandingMath.java
package com.kn8.core.soldier;

/**
 * Regras puras da mobilidade dos soldados (MOV-1, plano de mobilidade v3 §4): onde um impulso pode terminar, em que
 * ordem tentar as direcoes de esquiva e quando o soldado esta travado. A geometria do mundo fica no
 * {@code MoveSafety}; aqui so as decisoes, testadas no JUnit.
 */
public final class LandingMath {

    private LandingMath() {
    }

    /**
     * Primeira profundidade com apoio: {@code supportedAt[i]} = a caixa encosta em algo ao descer {@code i} blocos
     * (0 = chao logo abaixo dos pes). -1 sem apoio em toda a coluna (vao ou queda maior que o conferido).
     */
    public static int firstSupport(boolean[] supportedAt) {
        for (int i = 0; i < supportedAt.length; i++) {
            if (supportedAt[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Pouso aceitavel: o impulso chegou a pelo menos {@code minFraction} da distancia pedida (parede logo a frente
     * nao conta como esquiva) e a queda no fim cabe em {@code maxDrop} blocos.
     */
    public static boolean acceptable(double travelled, double wanted, double minFraction, int drop, int maxDrop) {
        return drop >= 0 && drop <= maxDrop && wanted > 0 && travelled >= wanted * minFraction;
    }

    /**
     * Ordem das direcoes de esquiva: o lado sorteado, o outro lado e, por ultimo, para tras (+1 direita, -1
     * esquerda, 0 tras). Para tras fica por ultimo: recuar em linha reta continua dentro de golpes em linha.
     */
    public static int[] dodgeOrder(boolean preferRight) {
        return preferRight ? new int[] {1, -1, 0} : new int[] {-1, 1, 0};
    }

    /** Travado: queria andar e, na janela inteira, saiu do lugar menos que {@code minMove} blocos. */
    public static boolean stuck(double moved, double minMove, boolean wantsToMove) {
        return wantsToMove && moved < minMove;
    }
}
