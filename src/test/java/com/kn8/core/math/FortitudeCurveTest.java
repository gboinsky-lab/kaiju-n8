// src/test/java/com/kn8/core/math/FortitudeCurveTest.java
package com.kn8.core.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Confere que os padroes do config reproduzem as tabelas do GDD (secoes 10 e 23). */
class FortitudeCurveTest {

    // Mesmos valores padrao definidos em ServerConfig (GDD secao 10).
    private static final FortitudeCurve GDD = new FortitudeCurve(20.0, 2.0, 2.0, 1.6, 2.0, 20.0);
    private static final double HEALTH_DELTA = 0.5;
    private static final double DAMAGE_DELTA = 0.05;
    private static final double EXACT = 1e-9;

    @Test
    void healthMatchesTheGddTables() {
        assertEquals(28, GDD.health(2.5), HEALTH_DELTA);     // Trichonephila
        assertEquals(211, GDD.health(5.4), HEALTH_DELTA);    // Primigenius
        assertEquals(422, GDD.health(6.4), HEALTH_DELTA);    // Honju revivido
        assertEquals(1280, GDD.health(8.0), HEALTH_DELTA);   // Terceira Onda
        assertEquals(2560, GDD.health(9.0), HEALTH_DELTA);   // numerados 11-15
        assertEquals(3880, GDD.health(9.6), 5.0);            // Nº 6 lendario
    }

    @Test
    void damageMatchesTheGddTables() {
        assertEquals(2.5, GDD.damage(2.5), DAMAGE_DELTA);
        assertEquals(9.9, GDD.damage(5.4), DAMAGE_DELTA);
        assertEquals(15.8, GDD.damage(6.4), DAMAGE_DELTA);
        assertEquals(33.6, GDD.damage(8.0), DAMAGE_DELTA);
    }

    @Test
    void armorIsCapped() {
        assertEquals(10.8, GDD.armor(5.4), EXACT);
        assertEquals(19.2, GDD.armor(9.6), EXACT);
        assertEquals(20.0, GDD.armor(12.0), EXACT);
    }

    @Test
    void referenceFortitudeGivesTheBaseValues() {
        assertEquals(20.0, GDD.health(2.0), EXACT);
        assertEquals(2.0, GDD.damage(2.0), EXACT);
    }

    @Test
    void rejectsCurvesThatWouldNotGrow() {
        assertThrows(IllegalArgumentException.class, () -> new FortitudeCurve(20, 1.0, 2, 1.6, 2, 20));
        assertThrows(IllegalArgumentException.class, () -> new FortitudeCurve(0, 2.0, 2, 1.6, 2, 20));
    }
}
