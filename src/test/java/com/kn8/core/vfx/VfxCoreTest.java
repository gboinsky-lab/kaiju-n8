package com.kn8.core.vfx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class VfxCoreTest {

    @Test
    void budgetScalesByQualityAndNeverPassesTheCap() {
        VfxBudget budget = new VfxBudget(30);
        assertEquals(7, budget.take(20, 0.35F), "baixo: 35%");
        assertEquals(20, budget.take(20, 1.0F));
        assertEquals(3, budget.take(20, 1.0F), "so o que sobra do teto");
        assertEquals(0, budget.take(20, 1.0F));
        assertEquals(1, new VfxBudget(10).take(1, 0.1F), "pediu alguma: pelo menos 1");
    }

    @Test
    void ringStaysFlatAndSpreadsEvenly() {
        Random random = new Random(1);
        for (int i = 0; i < 8; i++) {
            double[] p = VfxPatterns.spawn(VfxPatterns.Pattern.RING, i, 8, 0, 1, 2.0, 0.5, random::nextDouble);
            assertEquals(0.0, p[4], 1.0E-9, "anel no chao: sem velocidade vertical");
            assertEquals(0.5, Math.hypot(p[3], p[5]), 1.0E-9, "todos na mesma velocidade");
        }
    }

    @Test
    void lineGoesAlongTheDirectionAndConvergeComesBack() {
        Random random = new Random(2);
        double[] end = VfxPatterns.spawn(VfxPatterns.Pattern.LINE, 9, 10, 1, 0, 5.0, 0.0, random::nextDouble);
        assertEquals(5.0, end[0], 1.0E-9);
        assertEquals(0.0, end[2], 1.0E-9);
        double[] in = VfxPatterns.spawn(VfxPatterns.Pattern.CONVERGE, 0, 1, 1, 0, 3.0, 0.2, random::nextDouble);
        double dot = in[0] * in[3] + in[1] * in[4] + in[2] * in[5];
        assertTrue(dot < 0, "converge: a velocidade aponta para o centro");
        assertEquals(3.0, Math.sqrt(in[0] * in[0] + in[1] * in[1] + in[2] * in[2]), 1.0E-6);
        assertEquals(VfxPatterns.Pattern.ARC, VfxPatterns.Pattern.of("arc"));
        assertEquals(VfxPatterns.Pattern.BURST, VfxPatterns.Pattern.of("nada"));
    }

    @Test
    void colorsParseAndShade() {
        assertEquals(0x18E3E3, VfxColors.parse("#18E3E3"));
        assertEquals(-1, VfxColors.parse("#12"));
        assertEquals("#18E3E3", VfxColors.format(0x18E3E3));
        assertEquals(0xFFFFFF, VfxColors.shade(0x123456, 1.0F));
        assertEquals(0x000000, VfxColors.shade(0x123456, -1.0F));
    }
}
