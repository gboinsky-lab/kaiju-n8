// src/main/java/com/kn8/core/world/SiteTerrain.java
package com.kn8.core.world;

/**
 * Regra pura do terreno de um local da obra (construcoes C3), testavel com JUnit: amostras em grade em volta do
 * ponto; o local so nasce se no maximo {@code maxWet} amostras tiverem agua na superficie (riacho ou lagoa pequena,
 * que a construcao cobre; lago, rio largo ou mar passam disso) e o desnivel do chao seco (maior menos menor altura)
 * nao passar de {@code maxSlope}. Para na primeira amostra que reprova ({@link #add} devolve false).
 */
public final class SiteTerrain {

    private final int maxSlope;
    private final int maxWet;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;
    private int samples;
    private int wet;
    private boolean rejected;

    public SiteTerrain(int maxSlope, int maxWet) {
        this.maxSlope = maxSlope;
        this.maxWet = maxWet;
    }

    /** Quantas amostras molhadas a fracao permite numa grade de {@code samples} pontos. */
    public static int allowedWet(int samples, double fraction) {
        return (int) Math.floor(samples * fraction);
    }

    /** Uma amostra: altura do chao e se a superficie ali e agua (ou outro fluido). False = local reprovado. */
    public boolean add(int ground, boolean isWet) {
        if (rejected) {
            return false;
        }
        samples++;
        if (isWet) {
            rejected = ++wet > maxWet;
            return !rejected;
        }
        min = Math.min(min, ground);
        max = Math.max(max, ground);
        rejected = max - min > maxSlope;
        return !rejected;
    }

    public boolean accepted() {
        return !rejected && samples > wet;
    }

    public int slope() {
        return max < min ? 0 : max - min;
    }

    public int wet() {
        return wet;
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

    /** Algum ponto candidato (cx, cz) a ate {@code range} chunks de (x, z)? (vizinhanca quadrada, em chunks). */
    public static boolean within(int x, int z, int cx, int cz, int range) {
        return Math.abs(cx - x) <= range && Math.abs(cz - z) <= range;
    }
}
