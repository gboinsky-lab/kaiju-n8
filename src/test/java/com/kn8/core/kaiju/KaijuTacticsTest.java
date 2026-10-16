// src/test/java/com/kn8/core/kaiju/KaijuTacticsTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KaijuTacticsTest {

    @Test
    void neighbourIdsArriveFromDifferentSides() {
        // Ids seguidos: angulos separados por ~137,5 graus (nunca o mesmo lado).
        double a = KaijuTactics.spreadAngle(10);
        double b = KaijuTactics.spreadAngle(11);
        double diff = Math.abs(a - b);
        diff = Math.min(diff, Math.PI * 2.0 - diff);
        assertTrue(diff > Math.toRadians(90), "diferenca " + Math.toDegrees(diff));
    }

    @Test
    void orbitKeepsTheRadiusAndMovesSideways() {
        double[] p = KaijuTactics.orbit(0, 0, 5, 0, 5, Math.PI / 2, 1);
        assertEquals(0.0, p[0], 1e-9);
        assertEquals(5.0, p[1], 1e-9);
        double[] q = KaijuTactics.orbit(0, 0, 5, 0, 5, Math.PI / 2, -1);
        assertEquals(-5.0, q[1], 1e-9);
    }
}
