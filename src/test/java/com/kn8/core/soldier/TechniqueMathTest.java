package com.kn8.core.soldier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TechniqueMathTest {

    @Test
    void recedingTargetLeavesReach() {
        // Alvo a 3 blocos fugindo a 0,3 por tick: em 5 ticks esta a 4,5, fora de um combo de 3,5.
        double edge = TechniqueMath.predictedEdge(3.0, 0.3, 5);
        assertEquals(4.5, edge, 1.0E-9);
        assertFalse(TechniqueMath.stillInReach(edge, 3.5, 0.5));
    }

    @Test
    void approachingOrStillTargetKeepsDistance() {
        assertEquals(3.0, TechniqueMath.predictedEdge(3.0, -0.4, 5), 1.0E-9);
        assertEquals(3.0, TechniqueMath.predictedEdge(3.0, 0.0, 5), 1.0E-9);
        assertTrue(TechniqueMath.stillInReach(3.0, 3.5, 0.0));
    }

    @Test
    void readsTheWindupOnlyWhenTheHitComesFirst() {
        // Contra-golpe no tick 3 contra um pisao que chega em 10: sai antes (com 2 de folga).
        assertTrue(TechniqueMath.landsBefore(3, 1.0, 10, 2));
        // Estocada no tick 9: o pisao chega antes.
        assertFalse(TechniqueMath.landsBefore(9, 1.0, 10, 2));
        // Com o Release a 1,5x o golpe do tick 9 sai no 6.
        assertTrue(TechniqueMath.landsBefore(9, 1.5, 10, 2));
    }

    @Test
    void bestTotalPicksTheHardestHitter() {
        assertEquals(3.6, TechniqueMath.bestTotal(new double[] {2.5, 3.6, 1.3}), 1.0E-9);
        assertEquals(-1.0, TechniqueMath.bestTotal(new double[0]), 1.0E-9);
    }
}
