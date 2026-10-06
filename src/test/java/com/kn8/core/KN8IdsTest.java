// src/test/java/com/kn8/core/KN8IdsTest.java
package com.kn8.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KN8IdsTest {

    @Test
    void acceptsValidResourcePaths() {
        assertTrue(KN8Ids.isValidResourcePath("primigenius"));
        assertTrue(KN8Ids.isValidResourcePath("kaiju/revived_honju"));
        assertTrue(KN8Ids.isValidResourcePath("boss.exam-1"));
    }

    @Test
    void rejectsInvalidResourcePaths() {
        assertFalse(KN8Ids.isValidResourcePath(null));
        assertFalse(KN8Ids.isValidResourcePath(""));
        assertFalse(KN8Ids.isValidResourcePath("Primigenius"));
        assertFalse(KN8Ids.isValidResourcePath("kaiju no8"));
        assertFalse(KN8Ids.isValidResourcePath("kn8:primigenius"));
    }

    @Test
    void buildsAnimationNameByConvention() {
        assertEquals("primigenius.action.slam", KN8Ids.animationName("primigenius", "action", "slam"));
    }

    @Test
    void rejectsInvalidAnimationParts() {
        assertThrows(IllegalArgumentException.class, () -> KN8Ids.animationName("Primigenius", "action", "slam"));
        assertThrows(IllegalArgumentException.class, () -> KN8Ids.animationName("primigenius", "", "slam"));
        assertThrows(IllegalArgumentException.class, () -> KN8Ids.animationName("primigenius", "action", "big.slam"));
        assertThrows(NullPointerException.class, () -> KN8Ids.animationName("primigenius", null, "slam"));
    }
}
