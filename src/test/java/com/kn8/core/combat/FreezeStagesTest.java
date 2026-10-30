// src/test/java/com/kn8/core/combat/FreezeStagesTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FreezeStagesTest {

    @Test
    void slownessGrowsEachStageUpToTheShot() {
        assertEquals(3, FreezeStages.stages(3, 7));
        assertEquals(2, FreezeStages.stages(3, 2));
        assertEquals(3, FreezeStages.level(0, 3, 7));
        assertEquals(5, FreezeStages.level(1, 3, 7));
        assertEquals(7, FreezeStages.level(2, 3, 7));
        assertEquals(7, FreezeStages.level(0, 1, 7));
        assertEquals(1, FreezeStages.level(0, 2, 2));
        assertEquals(2, FreezeStages.level(1, 2, 2));
    }

    @Test
    void iceWalksFromTheHitToTheCenterAndEndsTogether() {
        assertEquals(0.0, FreezeStages.reach(0, 3), 1.0E-9);
        assertEquals(0.5, FreezeStages.reach(1, 3), 1.0E-9);
        assertEquals(1.0, FreezeStages.reach(2, 3), 1.0E-9);
        assertEquals(1.0, FreezeStages.reach(0, 1), 1.0E-9);
        assertEquals(100, FreezeStages.duration(0, 8, 100));
        assertEquals(84, FreezeStages.duration(2, 8, 100));
        assertEquals(1, FreezeStages.duration(5, 8, 10));
    }
}
