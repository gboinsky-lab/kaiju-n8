// src/test/java/com/kn8/core/combat/CombatMathTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class CombatMathTest {

    private static final float DELTA = 1e-4F;

    @Test
    void damageUsesWeaponActionComboAndRelease() {
        // Faca de combate: base 6, leve x1,0, terceiro golpe do combo x1,2, liberacao 50% -> x3.
        assertEquals(21.6F, CombatMath.damage(6, 1.0F, 1.2F, 3.0), DELTA);
        // Pesado x2,0 sem combo, 0% -> x1.
        assertEquals(12.0F, CombatMath.damage(6, 2.0F, 1.0F, 1.0), DELTA);
    }

    @Test
    void comboContinuesInsideTheWindowAndWraps() {
        assertEquals(1, CombatMath.nextComboStep(0, 100, 105, 10, 3));
        assertEquals(2, CombatMath.nextComboStep(1, 100, 110, 10, 3));
        assertEquals(0, CombatMath.nextComboStep(2, 100, 110, 10, 3));
        // Fora da janela: recomeca.
        assertEquals(0, CombatMath.nextComboStep(1, 100, 111, 10, 3));
        // Primeiro golpe (sem anterior).
        assertEquals(0, CombatMath.nextComboStep(-1, 0, 5, 10, 3));
    }

    @Test
    void comboMultiplierReadsTheList() {
        assertEquals(1.2F, CombatMath.comboMultiplier(List.of(1.0F, 1.0F, 1.2F), 2), DELTA);
        assertEquals(1.0F, CombatMath.comboMultiplier(List.of(), 5), DELTA);
    }

    @Test
    void noStaminaMakesActionsThirtyPercentSlower() {
        assertEquals(12, CombatMath.slowed(12, true, 1.3));
        assertEquals(16, CombatMath.slowed(12, false, 1.3));
        assertEquals(6, CombatMath.slowed(4, false, 1.3));
    }

    @Test
    void blockReducesDamageAndCostsStamina() {
        CombatMath.Block block = CombatMath.block(20, 0.7, 0.5);
        assertEquals(6.0F, block.damageTaken(), DELTA);
        assertEquals(7.0F, block.staminaCost(), DELTA);
    }

    @Test
    void parryWindowGrowsWithRelease() {
        // Janela base 3: bloqueio no tick 100, golpe no 103 = parry; no 104 nao.
        assertTrue(CombatMath.isParry(100, 103, 30, 3, 6, 60));
        assertFalse(CombatMath.isParry(100, 104, 30, 3, 6, 60));
        // Com 60% a janela vai para 6.
        assertTrue(CombatMath.isParry(100, 106, 60, 3, 6, 60));
        // Golpe antes do bloqueio nunca e parry.
        assertFalse(CombatMath.isParry(100, 99, 60, 3, 6, 60));
    }

    @Test
    void criticalMultipliesOnce() {
        assertEquals(15.0F, CombatMath.withCritical(10, true, 1.5), DELTA);
        assertEquals(10.0F, CombatMath.withCritical(10, false, 1.5), DELTA);
    }

    @Test
    void chargeGrowsFromMinimumToFull() {
        assertEquals(-1.0F, CombatMath.chargeFraction(5, 6, 30), DELTA);
        assertEquals(0.0F, CombatMath.chargeFraction(6, 6, 30), DELTA);
        assertEquals(0.5F, CombatMath.chargeFraction(18, 6, 30), DELTA);
        assertEquals(1.0F, CombatMath.chargeFraction(99, 6, 30), DELTA);
        assertEquals(1.0F, CombatMath.chargedMultiplier(0.0F, 2.5), DELTA);
        assertEquals(1.75F, CombatMath.chargedMultiplier(0.5F, 2.5), DELTA);
        assertEquals(2.5F, CombatMath.chargedMultiplier(1.0F, 2.5), DELTA);
    }

    @Test
    void releaseMakesAttacksFaster() {
        assertEquals(1.0, CombatMath.releaseSpeed(0, 0.5), DELTA);
        assertEquals(1.2, CombatMath.releaseSpeed(40, 0.5), DELTA);
        assertEquals(1.5, CombatMath.releaseSpeed(100, 0.5), DELTA);
        assertEquals(1.5, CombatMath.releaseSpeed(150, 0.5), DELTA);
        assertEquals(12, CombatMath.faster(12, 1.0));
        assertEquals(10, CombatMath.faster(12, 1.2));
        assertEquals(8, CombatMath.faster(12, 1.5));
        assertEquals(1, CombatMath.faster(1, 1.5));
        assertEquals(0, CombatMath.faster(0, 1.5));
    }

    @Test
    void frontCheck() {
        assertTrue(CombatMath.inFront(0, 1, 0.2, 0.9));
        assertFalse(CombatMath.inFront(0, 1, 0, -1));
    }
}
