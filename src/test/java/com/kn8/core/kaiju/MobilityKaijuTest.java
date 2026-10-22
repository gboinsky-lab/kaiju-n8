package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MobilityKaijuTest {

    @Test
    void failuresRestTheAbilityAfterTheLimitAndAHitResets() {
        AbilityFailures failures = new AbilityFailures();
        assertFalse(failures.record("bite", true, 3));
        assertFalse(failures.record("bite", true, 3));
        assertFalse(failures.record("bite", false, 3), "acerto zera");
        assertEquals(0, failures.streak("bite"));
        failures.record("bite", true, 3);
        failures.record("bite", true, 3);
        assertTrue(failures.record("bite", true, 3), "terceira falha seguida: descanso");
        assertEquals(0, failures.streak("bite"), "contagem zera junto");
        assertFalse(failures.record("slam", true, 0), "limite 0 desliga");
    }

    @Test
    void leapArcLandsWhereTheSpeedsWereComputedFor() {
        int ticks = 16;
        double h = AbilitySelection.leapHorizontalSpeed(10.0, ticks, 0.91);
        double v = AbilitySelection.leapVerticalSpeed(2.0, ticks, 0.08, 0.98);
        double[][] arc = LeapArc.points(h, v, 0.0, ticks, 0.08, 0.98, 0.91);
        double[] end = arc[ticks - 1];
        assertEquals(10.0, end[0], 1.0E-6);
        assertEquals(2.0, end[1], 1.0E-3);
        assertTrue(LeapArc.apex(arc) > 2.0, "o arco sobe acima do ponto final");
    }

    @Test
    void steeringTriesStraightFirstThenOverThenAround() {
        List<double[]> c = FlightSteer.candidates(10.0, 0.0, 0.0, 6.0, 5.0);
        assertEquals(6.0, c.get(0)[0], 1.0E-9);
        assertEquals(0.0, c.get(0)[1], 1.0E-9);
        assertEquals(5.0, c.get(1)[1], 1.0E-9, "segundo: por cima");
        double[] side = c.get(c.size() - 2);
        assertEquals(0.0, side[1], 1.0E-9, "por ultimo: contornar pelo lado");
        assertTrue(side[0] < 6.0 && Math.abs(side[2]) > 1.0);
        for (double[] p : c) {
            assertTrue(Math.hypot(p[0], p[2]) <= 6.0 + 1.0E-9);
        }
    }
}
