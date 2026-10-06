// src/main/java/com/kn8/core/ui/HudMath.java
package com.kn8.core.ui;

/**
 * Contas da HUD em Java puro (testaveis com JUnit): posicao do painel por canto da tela, fracao de barra e
 * suavizacao dos valores entre um envio do servidor e o seguinte (o canal privado manda no maximo 4 por segundo).
 */
public final class HudMath {

    /** Canto da tela onde o painel fica. */
    public enum Corner {
        TOP_LEFT(false, false),
        TOP_RIGHT(true, false),
        BOTTOM_LEFT(false, true),
        BOTTOM_RIGHT(true, true);

        private final boolean right;
        private final boolean bottom;

        Corner(boolean right, boolean bottom) {
            this.right = right;
            this.bottom = bottom;
        }
    }

    /** Posicao em coordenadas JA escaladas (o painel e desenhado depois de {@code pose().scale(scale)}). */
    public record Origin(int x, int y) {
    }

    private HudMath() {
    }

    /**
     * Posicao do painel. Nos cantos de baixo, o painel nao pode invadir a faixa central da hotbar e das barras de
     * status ({@code reservedCenterWidth} x {@code reservedBottomHeight}, em unidades da GUI sem a escala da HUD):
     * se nao couber ao lado da hotbar, sobe para cima dessa faixa (ajuste do M6).
     */
    public static Origin origin(Corner corner, int screenWidth, int screenHeight, int panelWidth, int panelHeight,
            double scale, int margin, int reservedCenterWidth, int reservedBottomHeight) {
        int width = (int) Math.floor(screenWidth / scale);
        int height = (int) Math.floor(screenHeight / scale);
        int x = corner.right ? width - panelWidth - margin : margin;
        int y = corner.bottom ? height - panelHeight - margin : margin;
        if (corner.bottom) {
            int halfReserved = (int) Math.ceil(reservedCenterWidth / 2.0 / scale);
            int center = width / 2;
            boolean fitsBeside = corner.right
                    ? x >= center + halfReserved + margin
                    : x + panelWidth <= center - halfReserved - margin;
            if (!fitsBeside) {
                y = height - (int) Math.ceil(reservedBottomHeight / scale) - panelHeight - margin;
            }
        }
        return new Origin(Math.max(0, x), Math.max(0, y));
    }

    /** Fracao 0..1 para preencher uma barra; maximo zero ou negativo vira barra vazia. */
    public static float fraction(float value, float max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, value / max));
    }

    /**
     * Aproxima o valor mostrado do valor real a cada quadro. Diferencas minimas encostam direto, para a barra nao
     * ficar "tremendo" perto do alvo.
     */
    public static float approach(float shown, float target, float factor) {
        float next = shown + (target - shown) * factor;
        return Math.abs(target - next) < 0.01F ? target : next;
    }
}
