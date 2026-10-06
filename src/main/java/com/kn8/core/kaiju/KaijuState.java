// src/main/java/com/kn8/core/kaiju/KaijuState.java
package com.kn8.core.kaiju;

/**
 * Estado de alto nivel de um kaiju (Fase 4, secao 7.2). Decide qual animacao toca e, nos proximos modulos, quais
 * habilidades podem comecar. Sincronizado com o cliente pelo indice desta enum.
 */
public enum KaijuState {
    IDLE,
    WANDER,
    CHASE,
    ATTACK,
    STAGGER,
    DEAD;

    public static KaijuState byIndex(int index) {
        KaijuState[] values = values();
        return index >= 0 && index < values.length ? values[index] : IDLE;
    }
}
