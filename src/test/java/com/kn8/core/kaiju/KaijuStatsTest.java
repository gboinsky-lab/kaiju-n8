// src/test/java/com/kn8/core/kaiju/KaijuStatsTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.kn8.core.math.FortitudeCurve;

class KaijuStatsTest {

    private static final FortitudeCurve GDD = new FortitudeCurve(20, 2, 2, 1.6, 2, 20);
    private static final OptionalDouble NONE = OptionalDouble.empty();
    private static final double DELTA = 0.05;

    @Test
    void curveGivesGddNumbers() {
        KaijuStats primigenius = KaijuStats.of(5.4, GDD, KaijuStats.Multipliers.NONE, NONE, NONE, NONE);
        assertEquals(211.1, primigenius.health(), DELTA);
        assertEquals(9.89, primigenius.damage(), DELTA);
        assertEquals(10.8, primigenius.armor(), DELTA);
    }

    @Test
    void multipliersApplyToHealthAndDamageButNotArmor() {
        // Dificuldade HARD do GDD: vida x1,4 e dano x1,3; multiplicador global de vida x2.
        KaijuStats.Multipliers hard = new KaijuStats.Multipliers(2.0, 1.0, 1.4, 1.3);
        KaijuStats stats = KaijuStats.of(5.4, GDD, hard, NONE, NONE, NONE);
        assertEquals(211.1 * 2.8, stats.health(), 0.2);
        assertEquals(9.89 * 1.3, stats.damage(), DELTA);
        assertEquals(10.8, stats.armor(), DELTA);
    }

    @Test
    void overridesReplaceTheCurveButStillGetMultipliers() {
        KaijuStats.Multipliers easy = new KaijuStats.Multipliers(1, 1, 0.7, 0.6);
        KaijuStats stats = KaijuStats.of(5.4, GDD, easy, OptionalDouble.of(500), OptionalDouble.of(20),
                OptionalDouble.of(4));
        assertEquals(350, stats.health(), DELTA);
        assertEquals(12, stats.damage(), DELTA);
        assertEquals(4, stats.armor(), DELTA);
    }
}
