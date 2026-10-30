// src/main/java/com/kn8/common/parkour/ParkourState.java
package com.kn8.common.parkour;

/** Estado continuo de parkour do jogador (0.5.0-E). {@link #NONE} = andando normal. */
public enum ParkourState {
    NONE,
    WALL_RUN,
    WALL_SLIDE,
    LEDGE_HANG,
    SLIDE,
    /** FIX-3 (Miguel, 2026-10-30: "escalar predios"): subindo a parede de frente, enquanto houver stamina. */
    CLIMB;

    private static final ParkourState[] VALUES = values();

    public static ParkourState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
    }

    /** Agarrado a uma parede ou borda (a queda nao conta e o wall-jump vale). */
    public boolean onWall() {
        return this == WALL_RUN || this == WALL_SLIDE || this == LEDGE_HANG || this == CLIMB;
    }
}
