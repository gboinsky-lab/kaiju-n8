// src/test/java/com/kn8/core/combat/SpecialGeometryTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpecialGeometryTest {

    private static final double DELTA = 1e-6;

    @Test
    void leapLandsAtTheStartHeightAfterTheGivenTicks() {
        double vertical = SpecialGeometry.leapVerticalSpeed(12);
        assertEquals(0.0, SpecialGeometry.leapHeight(vertical, 12), 1e-9);
        assertTrue(SpecialGeometry.leapHeight(vertical, 6) > 1.0, "O salto deveria subir mais de um bloco");
        // Horizontal: 5 blocos em 12 ticks com o freio do ar de 0,91 por tick.
        double speed = SpecialGeometry.leapHorizontalSpeed(5.0, 12);
        double x = 0;
        double v = speed;
        for (int i = 0; i < 12; i++) {
            x += v;
            v *= 0.91;
        }
        assertEquals(5.0, x, 1e-9);
    }

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
    void lungeHitsAlongTheLineOnly() {
        // Faixa de 5 blocos para +z, meia largura 1,3: zumbi a 3 blocos na linha entra; a 3 ao lado (borda 2,7) nao.
        assertTrue(SpecialGeometry.inLunge(0, 64, 0, 0, 1, 5, 1.3, -0.3, 64, 2.7, 0.3, 66, 3.3));
        assertFalse(SpecialGeometry.inLunge(0, 64, 0, 0, 1, 5, 1.3, 2.7, 64, 2.7, 3.3, 66, 3.3));
        // Atras de quem ataca e depois do fim da faixa: fora; kaiju largo cuja borda encosta no fim: dentro.
        assertFalse(SpecialGeometry.inLunge(0, 64, 0, 0, 1, 5, 1.3, -0.3, 64, -3.3, 0.3, 66, -2.7));
        assertFalse(SpecialGeometry.inLunge(0, 64, 0, 0, 1, 5, 1.3, -0.3, 64, 7.7, 0.3, 66, 8.3));
        assertTrue(SpecialGeometry.inLunge(0, 64, 0, 0, 1, 5, 1.3, -3, 64, 6, 3, 73, 12));
    }

    @Test
    void cooldownProgressGoesFromZeroToOne() {
        assertEquals(0.0F, SpecialGeometry.cooldownProgress(200, 200), 1e-6F);
        assertEquals(0.5F, SpecialGeometry.cooldownProgress(100, 200), 1e-6F);
        assertEquals(1.0F, SpecialGeometry.cooldownProgress(0, 200), 1e-6F);
        assertEquals(1.0F, SpecialGeometry.cooldownProgress(50, 0), 1e-6F);
    }
}
