// src/test/java/com/kn8/core/dismantle/DismantleMathTest.java
package com.kn8.core.dismantle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DismantleMathTest {

    @Test
    void sharesAddUpToTheTotal() {
        for (int total = 0; total <= 9; total++) {
            int sum = 0;
            for (int step = 1; step <= 5; step++) {
                sum += DismantleMath.share(total, step, 5);
            }
            assertEquals(total, sum, "total " + total);
        }
    }

    @Test
    void singleItemComesAtTheLastStep() {
        assertEquals(0, DismantleMath.share(1, 1, 3));
        assertEquals(0, DismantleMath.share(1, 2, 3));
        assertEquals(1, DismantleMath.share(1, 3, 3));
    }

    @Test
    void invalidStepsGiveNothing() {
        assertEquals(0, DismantleMath.share(5, 0, 3));
        assertEquals(0, DismantleMath.share(5, 4, 3));
        assertTrue(DismantleMath.stepComplete(40, 40));
        assertFalse(DismantleMath.stepComplete(36, 40));
    }
}
