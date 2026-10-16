package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LeapMathTest {

    @Test
    void vanillaJumpPeaksNearOnePointTwoFive() {
        assertEquals(1.25, LeapMath.peak(0.42), 0.02);
        assertEquals(0.42, LeapMath.verticalFor(0.5, 0.0), 1.0E-9);
    }

    @Test
    void verticalClearsTheHeightPlusMargin() {
        double v = LeapMath.verticalFor(3.0, 0.25);
        assertTrue(LeapMath.peak(v) >= 3.25);
        assertTrue(LeapMath.peak(v - 0.01) < 3.25);
    }

    @Test
    void landingTimeOnlyCountsWhenComingDown() {
        assertEquals(-1, LeapMath.ticksToReach(0.42, 2.0));
        int flat = LeapMath.ticksToReach(0.42, 0.0);
        int lower = LeapMath.ticksToReach(0.42, -2.0);
        assertTrue(flat > 8 && flat < 16, "pulo vanilla fica ~12 ticks no ar: " + flat);
        assertTrue(lower > flat);
    }

    @Test
    void horizontalSpeedCoversTheDistance() {
        double v = LeapMath.horizontalFor(4.0, 12);
        assertEquals(4.0, LeapMath.covered(v, 12), 1.0E-9);
        assertEquals(Double.POSITIVE_INFINITY, LeapMath.horizontalFor(1.0, 0));
    }

    @Test
    void feasibleLeaps() {
        assertTrue(LeapMath.feasible(2.0, 2.0, 1.5, 0.25), "subir num muro de 2");
        assertTrue(LeapMath.feasible(3.0, 3.0, 1.5, 0.25), "subir num muro de 3");
        assertTrue(LeapMath.feasible(0.0, 0.0, 4.0, 0.25), "vao de 4 no mesmo nivel");
        assertTrue(LeapMath.feasible(0.0, -2.0, 4.0, 0.25), "vao descendo 2");
        assertFalse(LeapMath.feasible(0.0, 0.0, 12.0, 0.25), "vao de 12 nao");
    }
}
