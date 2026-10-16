// src/main/java/com/kn8/core/world/SiteTerrain.java
package com.kn8.core.world;

/**
 * Regra pura do terreno de um local da obra (construcoes C3), testavel com JUnit: amostras em grade em volta do
 * ponto; o local so nasce se nenhuma amostra tiver agua na superficie e o desnivel do chao (maior menos menor
 * altura) nao passar de {@code maxSlope}. Para na primeira amostra que reprova ({@link #add} devolve false).
 */
public final class SiteTerrain {

    private final int maxSlope;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;
    private int samples;
    private boolean rejected;

    public SiteTerrain(int maxSlope) {
        this.maxSlope = maxSlope;
    }

    /** Uma amostra: altura do chao e se a superficie ali e agua (ou outro fluido). False = local reprovado. */
    public boolean add(int ground, boolean wet) {
        if (rejected) {
            return false;
        }
        samples++;
        min = Math.min(min, ground);
        max = Math.max(max, ground);
        rejected = wet || max - min > maxSlope;
        return !rejected;
    }

    public boolean accepted() {
        return !rejected && samples > 0;
    }

    public int slope() {
        return samples == 0 ? 0 : max - min;
    }

    /** Pontos (dx, dz) da grade de passo {@code step} dentro do circulo de raio {@code radius} (centro incluso). */
    public static int[][] grid(int radius, int step) {
        int n = radius / step;
        int count = 0;
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                if (i * i + j * j <= n * n) {
                    count++;
                }
            }
        }
        int[][] points = new int[count][];
        int k = 0;
        for (int i = -n; i <= n; i++) {
            for (int j = -n; j <= n; j++) {
                if (i * i + j * j <= n * n) {
                    points[k++] = new int[] {i * step, j * step};
                }
            }
        }
        return points;
    }
}
