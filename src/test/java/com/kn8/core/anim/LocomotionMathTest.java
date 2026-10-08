// src/test/java/com/kn8/core/anim/LocomotionMathTest.java
package com.kn8.core.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LocomotionMathTest {

    @Test
    void stoppedPlaysAtNormalSpeed() {
        assertEquals(1.0, LocomotionMath.animationSpeed(0.0, 32, 4.0, 0.35, 2.5));
    }

    @Test
    void feetFollowTheGround() {
        // 0,125 bloco/tick em 32 ticks = 4 blocos: passada de 4 -> 1,0; passada de 8 -> 0,5.
        assertEquals(1.0, LocomotionMath.animationSpeed(0.125, 32, 4.0, 0.35, 2.5), 1e-9);
        assertEquals(0.5, LocomotionMath.animationSpeed(0.125, 32, 8.0, 0.35, 2.5), 1e-9);
    }

    @Test
    void clampedBetweenMinAndMax() {
        assertEquals(0.35, LocomotionMath.animationSpeed(0.05, 32, 17.0, 0.35, 2.5), 1e-9);
        assertEquals(2.5, LocomotionMath.animationSpeed(1.0, 32, 2.0, 0.35, 2.5), 1e-9);
    }

    @Test
    void stepEveryHalfStride() {
        assertEquals(10.0F + 1.2F, LocomotionMath.nextStep(10.0F, 2.0F), 1e-6);
    }
}
