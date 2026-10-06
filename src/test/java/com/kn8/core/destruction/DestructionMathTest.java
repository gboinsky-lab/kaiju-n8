// src/test/java/com/kn8/core/destruction/DestructionMathTest.java
package com.kn8.core.destruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DestructionMathTest {

    @Test
    void tiersFollowHardness() {
        assertEquals(DestructionMath.FRAGILE, DestructionMath.tierFromHardness(0.2F));      // folhas
        assertEquals(DestructionMath.NORMAL, DestructionMath.tierFromHardness(2.0F));       // madeira
        assertEquals(DestructionMath.RESISTANT, DestructionMath.tierFromHardness(3.0F));    // pedra-ish
        assertEquals(DestructionMath.VERY_RESISTANT, DestructionMath.tierFromHardness(5.0F));
        assertEquals(DestructionMath.INDESTRUCTIBLE, DestructionMath.tierFromHardness(50.0F)); // obsidiana
        assertEquals(DestructionMath.INDESTRUCTIBLE, DestructionMath.tierFromHardness(-1.0F)); // bedrock
    }

    @Test
    void powerBreaksOnlyLowerTiers() {
        assertTrue(DestructionMath.canBreak(1, DestructionMath.FRAGILE));
        assertFalse(DestructionMath.canBreak(1, DestructionMath.NORMAL));
        assertTrue(DestructionMath.canBreak(3, DestructionMath.RESISTANT));
        assertFalse(DestructionMath.canBreak(4, DestructionMath.INDESTRUCTIBLE));
    }

    @Test
    void craterIsDeepestInTheCenterAndZeroOutside() {
        assertEquals(2.0, DestructionMath.craterDepth(0, 0, 4, 2, 7), 1e-9);
        assertEquals(0.0, DestructionMath.craterDepth(10, 0, 4, 2, 7), 1e-9);
        double mid = DestructionMath.craterDepth(1.5, 0, 4, 2, 7);
        assertTrue(mid > 0 && mid < 2.0);
    }

    @Test
    void craterEdgeIsIrregularButBounded() {
        boolean differs = false;
        double first = DestructionMath.edgeFactor(0, 42);
        for (int i = 0; i < 32; i++) {
            double factor = DestructionMath.edgeFactor(-Math.PI + i * Math.PI / 16, 42);
            assertTrue(factor >= 0.75 && factor <= 1.25);
            differs |= Math.abs(factor - first) > 0.01;
        }
        assertTrue(differs, "A borda deveria variar com o angulo");
    }
}
