package com.kn8.core.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** VFX-2: efeito cresce com o corpo (kaiju grande, efeito grande), dentro dos limites. */
class VfxScaleTest {

    @Test
    void humanIsTheReferenceAndBigKaijuGrow() {
        assertEquals(1.0F, VfxScale.body(1.8F, 1.8F), 1.0E-6F);
        float honju = VfxScale.body(8.5F, 1.8F);
        float giant = VfxScale.body(24.0F, 1.8F);
        assertTrue(honju > 4.0F && giant > honju);
        assertTrue(VfxScale.factor(giant, 0.75) > VfxScale.factor(honju, 0.75));
    }

    @Test
    void factorIsClampedAndUnknownHeightIsNeutral() {
        assertEquals(VfxScale.MAX, VfxScale.body(200.0F, 1.8F), 1.0E-6F);
        assertEquals(VfxScale.MIN, VfxScale.body(0.2F, 1.8F), 1.0E-6F);
        assertEquals(1.0F, VfxScale.body(0.0F, 1.8F), 1.0E-6F);
        // Quantidade cresce menos que o tamanho.
        assertTrue(VfxScale.factor(10.0F, 0.5) < VfxScale.factor(10.0F, 0.75));
    }
}
