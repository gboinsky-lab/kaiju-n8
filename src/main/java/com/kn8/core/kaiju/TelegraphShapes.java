// src/main/java/com/kn8/core/kaiju/TelegraphShapes.java
package com.kn8.core.kaiju;

/**
 * Formas do aviso de golpe perigoso (IA-6, plano de IA Parte 12: "preparacao visual, direcao ou area ameacada e tempo
 * aproximado de impacto") em Java puro: pontos (x, z) de um anel, de um arco e de uma linha, e a cor pelo tempo que
 * falta.
 */
public final class TelegraphShapes {

    private TelegraphShapes() {
    }

    /** {@code points} pontos num anel de raio {@code radius} em volta de (cx, cz). */
    public static double[][] ring(double cx, double cz, double radius, int points) {
        return arc(cx, cz, radius, 0.0, Math.PI * 2.0, points);
    }

    /**
     * {@code points} pontos num arco de raio {@code radius} em volta de (cx, cz), centrado no angulo {@code center}
     * (radianos, atan2 de z por x) com largura {@code width}.
     */
    public static double[][] arc(double cx, double cz, double radius, double center, double width, int points) {
        int count = Math.max(1, points);
        double[][] out = new double[count][];
        boolean full = width >= Math.PI * 2.0 - 1.0E-6;
        for (int i = 0; i < count; i++) {
            double t = full ? (double) i / count : count == 1 ? 0.5 : (double) i / (count - 1);
            double angle = center - width * 0.5 + width * t;
            out[i] = new double[] {cx + Math.cos(angle) * radius, cz + Math.sin(angle) * radius};
        }
        return out;
    }

    /** {@code points} pontos de (x0, z0) ate (x1, z1), inclusive. */
    public static double[][] line(double x0, double z0, double x1, double z1, int points) {
        int count = Math.max(2, points);
        double[][] out = new double[count][];
        for (int i = 0; i < count; i++) {
            double t = (double) i / (count - 1);
            out[i] = new double[] {x0 + (x1 - x0) * t, z0 + (z1 - z0) * t};
        }
        return out;
    }

    /**
     * Cor do aviso (r, g, b de 0 a 1): amarelo no inicio do preparo, vermelho no impacto ({@code ticksLeft} de
     * {@code windup}).
     */
    public static float[] color(int ticksLeft, int windup) {
        float progress = windup <= 0 ? 1.0F : 1.0F - Math.max(0, Math.min(windup, ticksLeft)) / (float) windup;
        return new float[] {1.0F, 0.85F * (1.0F - progress), 0.0F};
    }
}
