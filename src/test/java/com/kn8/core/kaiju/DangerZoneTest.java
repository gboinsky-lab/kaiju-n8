package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DangerZoneTest {

    @Test
    void discContainsAndExitsOutward() {
        DangerZone zone = DangerZone.disc(0, 0, 5);
        assertTrue(zone.contains(3, 0, 0));
        assertFalse(zone.contains(6, 0, 0));
        assertTrue(zone.contains(6, 0, 1.5), "margem");
        double[] exit = zone.exit(3, 0, 0, 0);
        assertEquals(1.0, exit[0], 1.0E-9);
        assertEquals(3.0, zone.exitDistance(3, 0, 1.0), 1.0E-9);
    }

    @Test
    void sectorOnlyInFront() {
        // Setor de 90 graus para +x.
        DangerZone zone = DangerZone.sector(0, 0, 6, 0.0, Math.PI * 0.5);
        assertTrue(zone.contains(4, 1, 0));
        assertFalse(zone.contains(-4, 0, 0), "atras do kaiju");
        assertFalse(zone.contains(0, 4, 0), "do lado");
    }

    @Test
    void sectorNearTheStraightEdgeExitsSideways() {
        DangerZone zone = DangerZone.sector(0, 0, 10, 0.0, Math.PI * 0.5);
        // Perto da borda de +45 graus, longe do arco: sai de lado (z cresce), nao para longe do centro.
        double[] exit = zone.exit(4, 3.6, 0, 0);
        assertTrue(exit[1] > 0.5, "sai pelo lado: " + exit[0] + ", " + exit[1]);
    }

    @Test
    void laneExitsSideways() {
        DangerZone zone = DangerZone.lane(0, 0, 10, 0, 1.5);
        assertTrue(zone.contains(5, 1, 0));
        assertFalse(zone.contains(5, 3, 0));
        assertFalse(zone.contains(13, 0, 0), "depois do fim da faixa");
        double[] exit = zone.exit(5, -0.5, 0, 0);
        assertEquals(-1.0, exit[1], 1.0E-9);
        assertEquals(1.0 + 1.0, zone.exitDistance(5, -0.5, 1.0), 1.0E-9);
    }

    @Test
    void centerUsesFallback() {
        double[] exit = DangerZone.disc(0, 0, 4).exit(0, 0, 0, 2);
        assertEquals(1.0, exit[1], 1.0E-9);
    }
}
