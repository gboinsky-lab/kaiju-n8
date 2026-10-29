// src/test/java/com/kn8/core/combat/TechniqueSelectionTest.java
package com.kn8.core.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TechniqueSelectionTest {

    @Test
    void cyclesForwardAndBackWithWrap() {
        assertEquals(1, TechniqueSelection.next(0, 1, 3));
        assertEquals(0, TechniqueSelection.next(2, 1, 3));
        assertEquals(2, TechniqueSelection.next(0, -1, 3));
        assertEquals(0, TechniqueSelection.next(0, 1, 1));
    }

    @Test
    void indexOutOfAShrunkListFallsBackToTheFirst() {
        assertEquals(0, TechniqueSelection.clamp(4, 2));
        assertEquals(0, TechniqueSelection.clamp(-1, 2));
        assertEquals(1, TechniqueSelection.clamp(1, 2));
        assertEquals(1, TechniqueSelection.next(5, 1, 2));
    }
}
