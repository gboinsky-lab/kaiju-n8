package com.kn8.core.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** VFX-R1: geometria dos efeitos desenhados (raio, arco, aparecer/sumir). */
class DrawMathTest {

    @Test
    void boltStartsAndEndsWhereItShould() {
        Random random = new Random(7);
        List<double[]> nodes = DrawMath.boltPath(new double[] {1, 2, 3}, new double[] {0, 0, 1}, 4.0, 6, 0.5,
                random::nextDouble);
        assertEquals(7, nodes.size());
        assertEquals(3.0, nodes.get(0)[2], 1.0E-9);
        assertEquals(7.0, nodes.get(6)[2], 1.0E-9);
        assertEquals(1.0, nodes.get(6)[0], 1.0E-9);
        // Os nos do meio desviam (zigue-zague), mas nunca mais que a amplitude.
        boolean bent = false;
        for (double[] node : nodes.subList(1, 6)) {
            bent |= Math.abs(node[0] - 1.0) > 1.0E-6;
            assertTrue(Math.abs(node[0] - 1.0) <= 0.5 + 1.0E-9 && Math.abs(node[1] - 2.0) <= 0.5 + 1.0E-9);
        }
        assertTrue(bent);
    }

    @Test
    void verticalArcRisesAndFalls() {
        double[] top = DrawMath.arcPoint(new double[] {0, 0, 0}, 0, 1, 90, 2.0, 90);
        double[] front = DrawMath.arcPoint(new double[] {0, 0, 0}, 0, 1, 90, 2.0, 0);
        assertEquals(2.0, top[1], 1.0E-9);
        assertEquals(2.0, front[2], 1.0E-9);
        assertEquals(0.0, front[1], 1.0E-9);
    }

    @Test
    void fadeInAndOut() {
        assertEquals(0.0F, DrawMath.fade(0.0F, 0.2F, 0.3F), 1.0E-6F);
        assertEquals(1.0F, DrawMath.fade(0.5F, 0.2F, 0.3F), 1.0E-6F);
        assertEquals(0.0F, DrawMath.fade(1.0F, 0.2F, 0.3F), 1.0E-6F);
        assertEquals(0.0F, DrawMath.fade(1.5F, 0.2F, 0.3F), 1.0E-6F);
    }
}
