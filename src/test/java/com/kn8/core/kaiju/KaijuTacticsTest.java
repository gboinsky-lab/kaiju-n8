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

    @Test
    void flankerGoesBehindAndAttackersWaitTheirTurn() {
        // Yaw 0 olha para +z: atras = -z.
        double[] behind = KaijuTactics.around(0, 0, KaijuTactics.behindAngle(0.0F), 4);
        assertEquals(0.0, behind[0], 1e-9);
        assertEquals(-4.0, behind[1], 1e-9);
        // Yaw 90 olha para -x: atras = +x.
        double[] side = KaijuTactics.around(0, 0, KaijuTactics.behindAngle(90.0F), 4);
        assertEquals(4.0, side[0], 1e-9);
        assertTrue(KaijuTactics.mustWait(3, false, 3), "3 colados e limite 3: espera");
        assertTrue(!KaijuTactics.mustWait(3, true, 3), "quem ja esta colado nao espera");
        assertTrue(!KaijuTactics.mustWait(2, false, 3), "ainda ha vaga");
        assertTrue(!KaijuTactics.mustWait(9, false, 0), "0 = sem limite");
        assertEquals(-1, KaijuTactics.supportMove(3.0, 8.0, 2.0));
        assertEquals(0, KaijuTactics.supportMove(7.0, 8.0, 2.0));
        assertEquals(1, KaijuTactics.supportMove(12.0, 8.0, 2.0));
    }
}
