// src/test/java/com/kn8/core/combat/ActionTimelineTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActionTimelineTest {

    // Valores do GDD (secao 9): golpe leve 12 ticks com impacto no 4; pesado 24 com impacto no 10.
    private static final int LIGHT_DURATION = 12;
    private static final int LIGHT_IMPACT = 4;

    @Test
    void impactHappensOnceAtTheImpactTick() {
        ActionTimeline timeline = new ActionTimeline();
        assertTrue(timeline.tryStart(100, "light", LIGHT_DURATION, LIGHT_IMPACT));
        assertFalse(timeline.consumeImpact(100));
        assertFalse(timeline.consumeImpact(103));
        assertTrue(timeline.consumeImpact(104));
        assertFalse(timeline.consumeImpact(104));
        assertFalse(timeline.consumeImpact(105));
        assertEquals(1, timeline.impacts());
    }

    @Test
    void lateTickStillProducesExactlyOneImpact() {
        // Servidor atrasado (lag): o impacto acontece no primeiro tick processado depois do previsto, uma vez so.
        ActionTimeline timeline = new ActionTimeline();
        timeline.tryStart(100, "light", LIGHT_DURATION, LIGHT_IMPACT);
        assertTrue(timeline.consumeImpact(110));
        assertFalse(timeline.consumeImpact(111));
    }

    @Test
    void newActionIsRejectedWhileTheCurrentOneIsActive() {
        ActionTimeline timeline = new ActionTimeline();
        assertTrue(timeline.tryStart(100, "light", LIGHT_DURATION, LIGHT_IMPACT));
        assertFalse(timeline.tryStart(105, "light", LIGHT_DURATION, LIGHT_IMPACT));
        assertFalse(timeline.tryStart(111, "heavy", 24, 10));
        assertTrue(timeline.tryStart(112, "light", LIGHT_DURATION, LIGHT_IMPACT));
        assertEquals(2, timeline.accepted());
        assertEquals(2, timeline.rejected());
        assertEquals(2, timeline.sequence());
    }

    @Test
    void spamProducesOneImpactPerAcceptedAction() {
        ActionTimeline timeline = new ActionTimeline();
        for (long tick = 0; tick < 120; tick++) {
            timeline.tryStart(tick, "light", LIGHT_DURATION, LIGHT_IMPACT); // tentativa em todo tick
            timeline.consumeImpact(tick);
        }
        assertEquals(10, timeline.accepted());
        assertEquals(10, timeline.impacts());
        assertEquals(110, timeline.rejected());
    }

    @Test
    void actionWithoutImpactNeverHits() {
        ActionTimeline timeline = new ActionTimeline();
        timeline.tryStart(0, "dodge", 6, ActionTimeline.NO_IMPACT);
        for (long tick = 0; tick < 10; tick++) {
            assertFalse(timeline.consumeImpact(tick));
        }
    }

    @Test
    void cancelledActionNeverHitsAndFreesTheTimeline() {
        ActionTimeline timeline = new ActionTimeline();
        timeline.tryStart(100, "bite", 8, 6);
        timeline.cancel();
        assertFalse(timeline.consumeImpact(106));
        assertFalse(timeline.isActive(101));
        assertTrue(timeline.tryStart(101, "bite", 8, 6));
    }

    @Test
    void invalidTimingIsRejected() {
        ActionTimeline timeline = new ActionTimeline();
        assertThrows(IllegalArgumentException.class, () -> timeline.tryStart(0, "x", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> timeline.tryStart(0, "x", 10, 10));
    }
}
