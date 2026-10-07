// src/test/java/com/kn8/core/combat/SpecialGeometryTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpecialGeometryTest {

    private static final double DELTA = 1e-6;

    @Test
    void distanceToBoxIsZeroInsideAndMeasuredFromTheEdge() {
        assertEquals(0.0, SpecialGeometry.horizontalDistanceToBox(0, 0, -1, -1, 1, 1), DELTA);
        assertEquals(2.0, SpecialGeometry.horizontalDistanceToBox(0, 0, 2, -1, 4, 1), DELTA);
        assertEquals(5.0, SpecialGeometry.horizontalDistanceToBox(0, 0, 3, 4, 6, 8), DELTA);
    }

    @Test
    void wideKaijuIsHitByItsEdgeNotItsCenter() {
        // Kaiju de 6 de largura com o centro a 6 blocos: a borda esta a 3, dentro do raio 4 do machado.
        assertTrue(SpecialGeometry.inGroundSlam(0, 64, 0, 4, 3, 64, -3, 9, 73, 3));
        // Zumbi a 5 blocos (borda a 4,7): fora.
        assertFalse(SpecialGeometry.inGroundSlam(0, 64, 0, 4, 4.7, 64, -0.3, 5.3, 66, 0.3));
    }

    @Test
    void targetsHighAboveOrBelowAreMissed() {
        assertFalse(SpecialGeometry.inGroundSlam(0, 64, 0, 4, -0.3, 68, -0.3, 0.3, 70, 0.3));
        assertFalse(SpecialGeometry.inGroundSlam(0, 64, 0, 4, -0.3, 60, -0.3, 0.3, 62.5, 0.3));
        assertTrue(SpecialGeometry.inGroundSlam(0, 64, 0, 4, -0.3, 63.5, -0.3, 0.3, 65.5, 0.3));
    }

    @Test
    void cooldownProgressGoesFromZeroToOne() {
        assertEquals(0.0F, SpecialGeometry.cooldownProgress(200, 200), 1e-6F);
        assertEquals(0.5F, SpecialGeometry.cooldownProgress(100, 200), 1e-6F);
        assertEquals(1.0F, SpecialGeometry.cooldownProgress(0, 200), 1e-6F);
        assertEquals(1.0F, SpecialGeometry.cooldownProgress(50, 0), 1e-6F);
    }
}
