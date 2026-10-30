// src/main/java/com/kn8/common/parkour/ParkourMove.java
package com.kn8.common.parkour;

/**
 * Intencao de parkour enviada pelo cliente (0.5.0-E). O cliente so diz o que tentou; o servidor confere a geometria,
 * a stamina e o estado e decide (regra 1). Impulsos (salto longo, vault, escalada, wall-jump, subir a borda) saem do
 * servidor como o dash; estados continuos (wall-run, wall-slide, segurar a borda, slide) sao aprovados pelo servidor
 * e aplicados tick a tick pelo cliente dono com os numeros que o servidor manda ({@link ParkourStateS2C}).
 */
public enum ParkourMove {
    LONG_JUMP,
    VAULT,
    CLIMB,
    WALL_RUN,
    WALL_SLIDE,
    LEDGE_GRAB,
    WALL_JUMP,
    LEDGE_CLIMB,
    SLIDE,
    /** Soltar o estado continuo (Shift na parede/borda, fim do slide pelo cliente). */
    STOP,
    /** FIX-3: subir a parede de frente (C segurado com a parede a frente). */
    WALL_CLIMB;

    private static final ParkourMove[] VALUES = values();

    /** Indice vindo da rede; fora da faixa vira {@link #STOP} (nada alem disso e aceito do cliente). */
    public static ParkourMove byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : STOP;
    }
}
