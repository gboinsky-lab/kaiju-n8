// src/test/java/com/kn8/core/kaiju/AbilitySelectionTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AbilitySelectionTest {

    @Test
    void highestPriorityWinsAndTiesAreDrawn() {
        assertEquals(1, AbilitySelection.pick(List.of(1, 5, 3), size -> 0));
        // Empate entre os indices 0 e 2 (prioridade 4): o sorteio escolhe o segundo empatado.
        assertEquals(2, AbilitySelection.pick(List.of(4, 1, 4), size -> 1));
        assertEquals(0, AbilitySelection.pick(List.of(4, 1, 4), size -> 0));
        assertEquals(-1, AbilitySelection.pick(List.of(), size -> 0));
    }

    @Test
    void rangeIsInclusive() {
        assertTrue(AbilitySelection.inRange(4.0, 4.0, 16.0));
        assertTrue(AbilitySelection.inRange(16.0, 4.0, 16.0));
        assertFalse(AbilitySelection.inRange(3.9, 4.0, 16.0));
    }

    @Test
    void arcCoversBehindAndSidesButNotTheFront() {
        // Rabada: 220 graus centrados atras (180).
        assertTrue(AbilitySelection.inArc(180.0, 180.0, 220.0));
        assertTrue(AbilitySelection.inArc(-90.0, 180.0, 220.0));
        assertTrue(AbilitySelection.inArc(80.0, 180.0, 220.0));
        assertFalse(AbilitySelection.inArc(0.0, 180.0, 220.0));
        assertFalse(AbilitySelection.inArc(60.0, 180.0, 220.0));
        assertTrue(AbilitySelection.inArc(123.0, 0.0, 360.0));
    }

    @Test
    void leapSpeedsReachTheTargetWithVanillaDrag() {
        double h = AbilitySelection.leapHorizontalSpeed(8.0, 16, 0.91);
        double travelled = 0;
        double v = h;
        for (int t = 0; t < 16; t++) {
            travelled += v;
            v *= 0.91;
        }
        assertEquals(8.0, travelled, 1e-6);
        double vy = AbilitySelection.leapVerticalSpeed(0.0, 16, 0.08, 0.98);
        double y = 0;
        double u = vy;
        for (int t = 0; t < 16; t++) {
            y += u;
            u = (u - 0.08) * 0.98;
        }
        assertEquals(0.0, y, 1e-6);
        assertTrue(vy > 0.5, "O salto deveria subir antes de cair");
    }
}
