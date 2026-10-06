// src/test/java/com/kn8/core/kaiju/KaijuBrainTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KaijuBrainTest {

    private static final double REACH_SQ = 9.0;

    private static KaijuState next(boolean dead, int stagger, boolean target, double distanceSq, boolean moving) {
        return KaijuBrain.next(new KaijuBrain.Inputs(dead, stagger, target, distanceSq, REACH_SQ, moving));
    }

    @Test
    void priorityOrder() {
        assertEquals(KaijuState.DEAD, next(true, 10, true, 1, true));
        assertEquals(KaijuState.STAGGER, next(false, 10, true, 1, true));
        assertEquals(KaijuState.ATTACK, next(false, 0, true, 9, true));
        assertEquals(KaijuState.CHASE, next(false, 0, true, 9.01, true));
        assertEquals(KaijuState.WANDER, next(false, 0, false, 0, true));
        assertEquals(KaijuState.IDLE, next(false, 0, false, 0, false));
    }

    @Test
    void invalidIndexFallsBackToIdle() {
        assertEquals(KaijuState.IDLE, KaijuState.byIndex(-1));
        assertEquals(KaijuState.IDLE, KaijuState.byIndex(99));
        assertEquals(KaijuState.CHASE, KaijuState.byIndex(KaijuState.CHASE.ordinal()));
    }
}
