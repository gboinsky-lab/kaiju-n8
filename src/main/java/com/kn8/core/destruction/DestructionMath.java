// src/main/java/com/kn8/core/destruction/DestructionMath.java
package com.kn8.core.destruction;

/**
 * Regras puras da destruicao controlada (Etapa E), testaveis com JUnit.
 *
 * <ul>
 *   <li>Categoria do bloco (0 = fragil, 1 = normal, 2 = resistente, 3 = muito resistente, 4 = indestrutivel): tags de
 *   bloco do JSON mandam; sem tag, sai da dureza do Minecraft ({@link #tierFromHardness}).</li>
 *   <li>Forca do ataque (1 a 4): quebra as categorias abaixo dela ({@link #canBreak}).</li>
 *   <li>Cratera irregular: a borda varia com o angulo (ruido determinista pela semente) e a profundidade cai do centro
 *   para a borda ({@link #craterDepth}).</li>
 * </ul>
 */
public final class DestructionMath {

    public static final int FRAGILE = 0;
    public static final int NORMAL = 1;
    public static final int RESISTANT = 2;
    public static final int VERY_RESISTANT = 3;
    public static final int INDESTRUCTIBLE = 4;

    private static final float FRAGILE_MAX = 0.5F;
    private static final float NORMAL_MAX = 2.5F;
    private static final float RESISTANT_MAX = 5.0F;
    private static final float VERY_RESISTANT_MAX = 50.0F;
    private static final int EDGE_BUCKETS = 16;
    private static final double EDGE_MIN = 0.75;
    private static final double EDGE_RANGE = 0.5;

    private DestructionMath() {
    }

    /** Categoria pela dureza do bloco (negativa = inquebravel, como bedrock). */
    public static int tierFromHardness(float hardness) {
        if (hardness < 0) {
            return INDESTRUCTIBLE;
        }
        if (hardness < FRAGILE_MAX) {
            return FRAGILE;
        }
        if (hardness < NORMAL_MAX) {
            return NORMAL;
        }
        if (hardness < RESISTANT_MAX) {
            return RESISTANT;
        }
        return hardness < VERY_RESISTANT_MAX ? VERY_RESISTANT : INDESTRUCTIBLE;
    }

    /** Forca 1 quebra so frageis; 2, ate normais; 3, ate resistentes; 4, ate muito resistentes. */
    public static boolean canBreak(int power, int tier) {
        return tier < INDESTRUCTIBLE && tier < power;
    }

    /**
     * Profundidade da cratera no ponto (dx, dz) a partir do centro, em blocos; 0 = fora da cratera. A borda e
     * irregular (entre 75% e 125% do raio, conforme o angulo), sem formato quadrado.
     */
    public static double craterDepth(double dx, double dz, double radius, double maxDepth, long seed) {
        double distance = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);
        double edge = radius * edgeFactor(angle, seed);
        if (distance >= edge) {
            return 0;
        }
        double t = distance / edge;
        return maxDepth * (1 - t * t);
    }

    /** Fator da borda no angulo: interpola entre "baldes" de ruido, para a borda variar suave. */
    static double edgeFactor(double angle, long seed) {
        double position = (angle + Math.PI) / (2 * Math.PI) * EDGE_BUCKETS;
        int bucket = (int) Math.floor(position) % EDGE_BUCKETS;
        int next = (bucket + 1) % EDGE_BUCKETS;
        double t = position - Math.floor(position);
        return EDGE_MIN + EDGE_RANGE * (noise(bucket, seed) * (1 - t) + noise(next, seed) * t);
    }

    /** Ruido determinista em [0, 1). */
    static double noise(int bucket, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L + bucket * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        return (h >>> 11) / (double) (1L << 53);
    }
}
