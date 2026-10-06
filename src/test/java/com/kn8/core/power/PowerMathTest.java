// src/test/java/com/kn8/core/power/PowerMathTest.java
package com.kn8.core.power;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Confere as formulas contra os numeros do GDD (secoes 5 a 8). */
class PowerMathTest {

    // Mesmos padroes de ServerConfig (GDD).
    static final PowerParams GDD = new PowerParams(25, 0.004, 0.004, 0.4, 0.005, 20,
            100, 0.5, 15, 20, 0.75,
            40, 70, 90, 100, 2.0, 10, 3,
            100, 1.0,
            50, 10);
    private static final double DELTA = 1e-9;

    @Test
    void damageMatchesGddTable() {
        assertEquals(1.04, PowerMath.damageMultiplier(1, GDD), DELTA);
        assertEquals(3.0, PowerMath.damageMultiplier(50, GDD), DELTA);
        assertEquals(5.0, PowerMath.damageMultiplier(100, GDD), DELTA);
    }

    @Test
    void speedReductionAndKnockbackScale() {
        assertEquals(0.4, PowerMath.speedBonus(100, GDD), DELTA);
        assertEquals(0.2, PowerMath.damageReduction(50, GDD), DELTA);
        assertEquals(0.4, PowerMath.damageReduction(100, GDD), DELTA);
        assertEquals(0.5, PowerMath.knockbackResistance(100, GDD), DELTA);
    }

    @Test
    void staminaMaxAndRegen() {
        assertEquals(150, PowerMath.maxStamina(100, GDD), DELTA);
        // Antes do atraso de 20 ticks nao regenera.
        assertEquals(50, PowerMath.staminaAfterTick(50, 100, 10, HeatStage.NORMAL, GDD), DELTA);
        // 15 por segundo = 0,75 por tick.
        assertEquals(50.75, PowerMath.staminaAfterTick(50, 100, 20, HeatStage.NORMAL, GDD), DELTA);
        // WARM: 25% mais devagar.
        assertEquals(50.5625, PowerMath.staminaAfterTick(50, 100, 20, HeatStage.WARM, GDD), DELTA);
        assertEquals(100, PowerMath.staminaAfterTick(99.9, 100, 40, HeatStage.NORMAL, GDD), DELTA);
    }

    @Test
    void heatStagesFollowTheGdd() {
        assertEquals(HeatStage.NORMAL, PowerMath.heatStage(39.9, GDD));
        assertEquals(HeatStage.WARM, PowerMath.heatStage(40, GDD));
        assertEquals(HeatStage.OVERLOAD, PowerMath.heatStage(70, GDD));
        assertEquals(HeatStage.CRITICAL, PowerMath.heatStage(90, GDD));
        assertEquals(HeatStage.PANIC, PowerMath.heatStage(100, GDD));
    }

    @Test
    void surgeHeatsAndRestCools() {
        // Surto de 20 = 2 x 2/s = 4/s = 0,2 por tick.
        assertEquals(10.2, PowerMath.heatAfterTick(10, 20, true, GDD), DELTA);
        // Fora de combate esfria 10/s = 0,5 por tick; em combate 3/s = 0,15.
        assertEquals(9.5, PowerMath.heatAfterTick(10, 0, false, GDD), DELTA);
        assertEquals(9.85, PowerMath.heatAfterTick(10, 0, true, GDD), DELTA);
        assertEquals(0, PowerMath.heatAfterTick(0.1, 0, false, GDD), DELTA);
        assertEquals(100, PowerMath.heatAfterTick(99.99, 20, true, GDD), DELTA);
    }

    @Test
    void effectiveReleaseRespectsCapAndSurgeLimit() {
        assertEquals(10, PowerMath.effectiveRelease(25, 10, 0, GDD));
        assertEquals(30, PowerMath.effectiveRelease(25, 10, 20, GDD));
        assertEquals(30, PowerMath.effectiveRelease(25, 10, 50, GDD));
        assertEquals(100, PowerMath.effectiveRelease(100, 100, 20, GDD));
    }

    @Test
    void trainingConvertsUpToTheCapAndKeepsOnlyOnePointInReserve() {
        // 0 -> 2: 50 + 60 = 110; sobram 90 (o ponto 3 custaria 70, entao converte mais um: sobram 20).
        assertEquals(new PowerMath.Training(3, 20), PowerMath.convertTraining(0, 200, 10, GDD));
        // No teto 10, 99050 XP viram reserva de um ponto (150).
        assertEquals(new PowerMath.Training(10, 150), PowerMath.convertTraining(10, 99_050, 10, GDD));
        // Teto sobe para 100: a reserva vira exatamente +1 ponto.
        assertEquals(new PowerMath.Training(11, 0), PowerMath.convertTraining(10, 150, 100, GDD));
    }

    @Test
    void trainingGetsMoreExpensive() {
        assertEquals(50, PowerMath.xpForNextPoint(0, GDD));
        assertEquals(150, PowerMath.xpForNextPoint(10, GDD));
    }
}
