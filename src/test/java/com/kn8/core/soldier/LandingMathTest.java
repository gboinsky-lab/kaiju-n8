// src/test/java/com/kn8/core/soldier/LandingMathTest.java
package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LandingMathTest {

    @Test
    void firstSupportFindsTheShallowestGround() {
        assertEquals(0, LandingMath.firstSupport(new boolean[] {true, true, false}));
        assertEquals(2, LandingMath.firstSupport(new boolean[] {false, false, true, true}));
        assertEquals(-1, LandingMath.firstSupport(new boolean[] {false, false, false}));
    }

    @Test
    void landingNeedsGroundWithinTheDropAndMostOfTheDistance() {
        assertTrue(LandingMath.acceptable(4.0, 5.0, 0.5, 0, 3));
        assertTrue(LandingMath.acceptable(2.5, 5.0, 0.5, 3, 3));
        // Queda maior que o permitido, vao sem chao e parede logo a frente.
        assertFalse(LandingMath.acceptable(5.0, 5.0, 0.5, 4, 3));
        assertFalse(LandingMath.acceptable(5.0, 5.0, 0.5, -1, 3));
        assertFalse(LandingMath.acceptable(1.0, 5.0, 0.5, 0, 3));
    }

    @Test
    void dodgeTriesBothSidesBeforeBackingOff() {
        assertArrayEquals(new int[] {1, -1, 0}, LandingMath.dodgeOrder(true));
        assertArrayEquals(new int[] {-1, 1, 0}, LandingMath.dodgeOrder(false));
    }

    @Test
    void stuckOnlyWhenTryingToMove() {
        assertTrue(LandingMath.stuck(0.3, 1.0, true));
        assertFalse(LandingMath.stuck(0.3, 1.0, false));
        assertFalse(LandingMath.stuck(2.0, 1.0, true));
    }
}
