// src/test/java/com/kn8/core/parkour/ParkourMathTest.java
package com.kn8.core.parkour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Parkour do jogador (0.5.0-E): classificacao do obstaculo, impulsos e curvas dos estados continuos. */
class ParkourMathTest {

    private static final double DELTA = 1e-9;

    @Test
    void obstacleHeightDecidesVaultOrClimb() {
        assertEquals(ParkourMath.Obstacle.NONE, ParkourMath.classify(0.5, 0.6, 1.5, 2.5));
        assertEquals(ParkourMath.Obstacle.VAULT, ParkourMath.classify(1.0, 0.6, 1.5, 2.5));
        assertEquals(ParkourMath.Obstacle.VAULT, ParkourMath.classify(1.5, 0.6, 1.5, 2.5));
        assertEquals(ParkourMath.Obstacle.CLIMB, ParkourMath.classify(2.0, 0.6, 1.5, 2.5));
        assertEquals(ParkourMath.Obstacle.NONE, ParkourMath.classify(3.0, 0.6, 1.5, 2.5));
    }

    @Test
    void vanillaJumpReachesAboutOneAndAQuarterBlocks() {
        // Pulo vanilla: 0,42 de velocidade vertical chega a ~1,25 bloco.
        double peak = ParkourMath.peakHeight(0.42);
        assertTrue(peak > 1.2 && peak < 1.3, "pico do pulo vanilla: " + peak);
    }

    @Test
    void launchSpeedClearsTheObstacle() {
        double speed = ParkourMath.launchSpeedFor(2.0, 0.2);
        assertTrue(ParkourMath.peakHeight(speed) >= 2.2, "deveria passar de 2,2 blocos");
        assertTrue(ParkourMath.peakHeight(speed - 0.01) < 2.2, "e ser o menor impulso que passa");
    }

    @Test
    void wallRunArcsUpThenDown() {
        assertEquals(0.12, ParkourMath.wallRunVertical(0, 20, 0.12), DELTA);
        assertEquals(0.0, ParkourMath.wallRunVertical(10, 20, 0.12), DELTA);
        assertEquals(-0.12, ParkourMath.wallRunVertical(20, 20, 0.12), DELTA);
        assertEquals(-0.12, ParkourMath.wallRunVertical(40, 20, 0.12), DELTA);
    }

    @Test
    void wallSlideCapsTheFall() {
        assertEquals(-0.1, ParkourMath.wallSlideVertical(-0.8, 0.1), DELTA);
        assertEquals(0.3, ParkourMath.wallSlideVertical(0.3, 0.1), DELTA);
    }

    @Test
    void slideSlowsDown() {
        assertEquals(1.0, ParkourMath.slideSpeed(1.0, 0, 0.9), DELTA);
        assertEquals(0.81, ParkourMath.slideSpeed(1.0, 2, 0.9), DELTA);
    }

    @Test
    void rollRemovesSmallFallsAndCutsBigOnes() {
        assertEquals(0.0F, ParkourMath.rollDamageMultiplier(5.0F, 6.0F, 0.6F), 1e-6);
        assertEquals(0.4F, ParkourMath.rollDamageMultiplier(10.0F, 6.0F, 0.6F), 1e-6);
    }

    @Test
    void agilityLowersTheCost() {
        assertEquals(15.0, ParkourMath.cost(20.0, 0.75), DELTA);
        assertEquals(0.0, ParkourMath.cost(20.0, -1.0), DELTA);
    }

    @Test
    void strongerPlayersJumpFurther() {
        assertEquals(1.0, ParkourMath.levelFactor(0, 100, 0, 0.3, 0.3), DELTA);
        assertEquals(1.15, ParkourMath.levelFactor(50, 100, 0, 0.3, 0.3), DELTA);
        assertEquals(1.6, ParkourMath.levelFactor(100, 100, 100, 0.3, 0.3), DELTA);
        assertEquals(1.6, ParkourMath.levelFactor(300, 100, 250, 0.3, 0.3), DELTA);
    }

    @Test
    void autoLedgeOnlyAtHandHeightMovingForwardAndNotRising() {
        assertTrue(ParkourMath.autoLedge(1.5, -0.2, true, 1.2, 2.0, 0.1));
        assertFalse(ParkourMath.autoLedge(1.5, -0.2, false, 1.2, 2.0, 0.1));
        assertFalse(ParkourMath.autoLedge(1.5, 0.4, true, 1.2, 2.0, 0.1));
        assertFalse(ParkourMath.autoLedge(0.8, -0.2, true, 1.2, 2.0, 0.1));
        assertFalse(ParkourMath.autoLedge(2.6, -0.2, true, 1.2, 2.0, 0.1));
    }

    @Test
    void climbGoesUpHoldsOrSlidesDown() {
        assertEquals(0.14, ParkourMath.climbVertical(1.0F, 0.14), 1.0E-9);
        assertEquals(0.0, ParkourMath.climbVertical(0.0F, 0.14), 1.0E-9);
        assertEquals(-0.14, ParkourMath.climbVertical(-1.0F, 0.14), 1.0E-9);
    }
}
