// src/main/java/com/kn8/core/combat/TechniqueSelection.java
package com.kn8.core.combat;

/**
 * COMBATE-1: escolha da tecnica do jogador na lista da arma (logica pura, JUnit). A lista da a volta: depois da
 * ultima vem a primeira.
 */
public final class TechniqueSelection {

    private TechniqueSelection() {
    }

    /** Indice depois de andar {@code delta} na lista de {@code count} tecnicas (com volta). */
    public static int next(int index, int delta, int count) {
        if (count <= 0) {
            return 0;
        }
        return Math.floorMod(clamp(index, count) + delta, count);
    }

    /** Indice valido para {@code count} tecnicas (a lista pode encolher num reload dos dados: volta a primeira). */
    public static int clamp(int index, int count) {
        return index >= 0 && index < count ? index : 0;
    }
}
