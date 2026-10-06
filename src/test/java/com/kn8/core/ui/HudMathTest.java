// src/test/java/com/kn8/core/ui/HudMathTest.java
package com.kn8.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HudMathTest {

    private static final int SCREEN_W = 480;
    private static final int SCREEN_H = 270;
    private static final int PANEL_W = 150;
    private static final int PANEL_H = 40;
    private static final int MARGIN = 6;
    private static final int HOTBAR = 182;
    private static final int STATUS = 65;
    private static final float DELTA = 1e-6F;

    private static HudMath.Origin origin(HudMath.Corner corner, int width, int height, double scale) {
        return HudMath.origin(corner, width, height, PANEL_W, PANEL_H, scale, MARGIN, HOTBAR, STATUS);
    }

    @Test
    void topCornersIgnoreTheHotbar() {
        assertEquals(new HudMath.Origin(6, 6), origin(HudMath.Corner.TOP_LEFT, SCREEN_W, SCREEN_H, 1.0));
        assertEquals(new HudMath.Origin(324, 6), origin(HudMath.Corner.TOP_RIGHT, SCREEN_W, SCREEN_H, 1.0));
    }

    @Test
    void narrowScreenLiftsBottomPanelAboveTheStatusBars() {
        // 480 de largura: 6 + 150 > 240 - 91 - 6, entao o painel sobe: 270 - 65 - 40 - 6 = 159.
        assertEquals(new HudMath.Origin(6, 159), origin(HudMath.Corner.BOTTOM_LEFT, SCREEN_W, SCREEN_H, 1.0));
        assertEquals(new HudMath.Origin(324, 159), origin(HudMath.Corner.BOTTOM_RIGHT, SCREEN_W, SCREEN_H, 1.0));
    }

    @Test
    void wideScreenKeepsBottomPanelBesideTheHotbar() {
        // 960 de largura: 156 <= 480 - 91 - 6, cabe ao lado da hotbar e fica no rodape: 540 - 40 - 6 = 494.
        assertEquals(new HudMath.Origin(6, 494), origin(HudMath.Corner.BOTTOM_LEFT, 960, 540, 1.0));
        assertEquals(new HudMath.Origin(804, 494), origin(HudMath.Corner.BOTTOM_RIGHT, 960, 540, 1.0));
    }

    @Test
    void scaleShrinksTheReservedArea() {
        // Escala 2 numa tela 960x540: espaco escalado 480x270; reserva vira 91 x 33.
        // 156 > 240 - 46 - 6 = 188? nao: 156 <= 188, entao cabe ao lado: y = 270 - 40 - 6 = 224.
        assertEquals(new HudMath.Origin(6, 224), origin(HudMath.Corner.BOTTOM_LEFT, 960, 540, 2.0));
    }

    @Test
    void panelNeverLeavesTheScreen() {
        assertEquals(new HudMath.Origin(0, 0), origin(HudMath.Corner.BOTTOM_RIGHT, 100, 30, 1.0));
    }

    @Test
    void fractionIsClamped() {
        assertEquals(0.5F, HudMath.fraction(50, 100), DELTA);
        assertEquals(1F, HudMath.fraction(150, 100), DELTA);
        assertEquals(0F, HudMath.fraction(-5, 100), DELTA);
        assertEquals(0F, HudMath.fraction(10, 0), DELTA);
    }

    @Test
    void approachMovesTowardsTargetAndSnaps() {
        assertEquals(75F, HudMath.approach(50, 100, 0.5F), DELTA);
        assertEquals(100F, HudMath.approach(99.995F, 100, 0.5F), DELTA);
    }
}
