// src/test/java/com/kn8/core/kaiju/KaijuScaleTest.java
package com.kn8.core.kaiju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KaijuScaleTest {

    @Test
    void currentSpeciesFitTheirCategory() {
        // Trichonephila 3,0 x 3,4: no limite da folga do Yoju (4 - 15% = 3,4).
        assertTrue(KaijuScale.fits("yoju", 3.0, 3.4));
        assertTrue(KaijuScale.fits("yoju", 3.13, 6.0));
        assertTrue(KaijuScale.fits("honju", 5.73, 9.0));
    }

    @Test
    void outOfRangeIsReported() {
        assertFalse(KaijuScale.fits("yoju", 1.6, 1.2));
        assertFalse(KaijuScale.fits("honju", 3.5, 5.0));
        assertFalse(KaijuScale.fits("daikaiju", 9, 9));
        // Categoria sem faixa (numerados) e sempre aceita.
        assertTrue(KaijuScale.fits("numbered", 1, 2));
    }

    @Test
    void sizeClasses() {
        assertEquals(KaijuScale.SizeClass.SMALL, KaijuScale.sizeClass("yoju", 3.0, 3.4).orElseThrow());
        assertEquals(KaijuScale.SizeClass.MEDIUM, KaijuScale.sizeClass("yoju", 3.13, 6.0).orElseThrow());
        assertEquals(KaijuScale.SizeClass.MEDIUM, KaijuScale.sizeClass("honju", 5.73, 9.0).orElseThrow());
        assertEquals(KaijuScale.SizeClass.LARGE, KaijuScale.sizeClass("daikaiju", 10, 29).orElseThrow());
    }
}
