// src/test/java/com/kn8/core/kaiju/AbilityGeometryTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AbilityGeometryTest {

    private static final double RADIUS = 3.0;
    private static final double PLAYER_WIDTH = 0.6;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double KAIJU_HEIGHT = 4.6;

    @Test
    void centerSitsOnTheFrontEdge() {
        assertEquals(1.2, AbilityGeometry.slamCenterForward(2.4), 1e-9);
    }

    @Test
    void radiusIsMeasuredToTheTargetsEdge() {
        // Centro do jogador a 3,3: borda a 3,0 -> dentro. A 3,31 -> fora.
        assertTrue(AbilityGeometry.inSlamArea(0, 3.3, 0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
        assertFalse(AbilityGeometry.inSlamArea(0, 3.31, 0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
        // Diagonal: (2, 2) = 2,83 do centro, borda a 2,53.
        assertTrue(AbilityGeometry.inSlamArea(2, 2, 0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
    }

    @Test
    void targetsAboveTheKaijuOrBelowTheGroundAreSafe() {
        assertFalse(AbilityGeometry.inSlamArea(0, 1, 5.0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
        assertFalse(AbilityGeometry.inSlamArea(0, 1, -2.0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
        assertTrue(AbilityGeometry.inSlamArea(0, 1, 4.0, PLAYER_WIDTH, PLAYER_HEIGHT, RADIUS, KAIJU_HEIGHT));
    }
}
