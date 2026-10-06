// src/test/java/com/kn8/core/anim/AnimTimingTest.java
package com.kn8.core.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AnimTimingTest {

    @Test
    void noDelayStartsFromTheBeginning() {
        assertEquals(0, AnimTiming.catchUpTicks(100, 100, 6));
        // Relogio do cliente um pouco adiantado nunca faz a animacao "voltar".
        assertEquals(0, AnimTiming.catchUpTicks(100, 99, 6));
    }

    @Test
    void lateClientSkipsTheDelay() {
        assertEquals(3, AnimTiming.catchUpTicks(100, 103, 6));
    }

    @Test
    void catchUpIsCapped() {
        assertEquals(6, AnimTiming.catchUpTicks(100, 140, 6));
        assertEquals(0, AnimTiming.catchUpTicks(100, 140, 0));
    }

    @Test
    void rawDelayIsReported() {
        assertEquals(-2, AnimTiming.delayTicks(100, 98));
        assertEquals(5, AnimTiming.delayTicks(100, 105));
    }
}
