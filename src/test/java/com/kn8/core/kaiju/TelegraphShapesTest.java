// src/test/java/com/kn8/core/kaiju/TelegraphShapesTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TelegraphShapesTest {

    @Test
    void ringPointsAreOnTheRadius() {
        double[][] ring = TelegraphShapes.ring(10, -4, 3, 12);
        assertEquals(12, ring.length);
        for (double[] point : ring) {
            assertEquals(3.0, Math.hypot(point[0] - 10, point[1] + 4), 1e-9);
        }
    }

    @Test
    void arcStaysInsideItsWidth() {
        double[][] arc = TelegraphShapes.arc(0, 0, 5, 0.0, Math.PI / 2, 9);
        for (double[] point : arc) {
            assertTrue(Math.abs(Math.atan2(point[1], point[0])) <= Math.PI / 4 + 1e-9);
        }
        assertEquals(5.0, arc[4][0], 1e-9);
    }

    @Test
    void lineGoesFromStartToEnd() {
        double[][] line = TelegraphShapes.line(0, 0, 10, 0, 6);
        assertEquals(0.0, line[0][0], 1e-9);
        assertEquals(10.0, line[5][0], 1e-9);
    }

    @Test
    void colorTurnsRedNearTheImpact() {
        float[] start = TelegraphShapes.color(20, 20);
        float[] impact = TelegraphShapes.color(0, 20);
        assertTrue(start[1] > 0.8F);
        assertEquals(0.0F, impact[1], 1e-6F);
        assertEquals(1.0F, impact[0], 1e-6F);
    }
}
