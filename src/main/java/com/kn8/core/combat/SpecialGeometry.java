// src/main/java/com/kn8/core/combat/SpecialGeometry.java
package com.kn8.core.combat;

/**
 * Geometria dos ataques especiais das armas em Java puro (0.5), testavel com JUnit.
 *
 * <p>Golpe no chao ({@code ground_slam}): o centro fica {@code forward} blocos a frente de quem ataca. Atinge quem
 * tem a hitbox (caixa inteira, nao o centro: kaiju sao largos) a ate {@code radius} no plano horizontal e esta na
 * faixa de altura do golpe (um pouco abaixo do chao ate {@link #SLAM_HEIGHT} acima).</p>
 */
public final class SpecialGeometry {

    /** Altura da onda do golpe no chao acima do ponto de impacto (pega um kaiju alto pelas pernas). */
    public static final double SLAM_HEIGHT = 3.0;
    /** Tolerancia abaixo do ponto de impacto (degrau, bloco meio afundado). */
    public static final double SLAM_BELOW = 1.0;

    private SpecialGeometry() {
    }

    /** Distancia horizontal do ponto (x, z) ate a caixa [minX, maxX] x [minZ, maxZ] (0 se estiver dentro). */
    public static double horizontalDistanceToBox(double x, double z, double minX, double minZ, double maxX,
            double maxZ) {
        double dx = Math.max(Math.max(minX - x, 0.0), x - maxX);
        double dz = Math.max(Math.max(minZ - z, 0.0), z - maxZ);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * O alvo esta na area do golpe no chao?
     *
     * @param centerY altura do ponto de impacto (pes de quem ataca)
     * @param minY    base da hitbox do alvo
     * @param maxY    topo da hitbox do alvo
     */
    public static boolean inGroundSlam(double centerX, double centerY, double centerZ, double radius, double minX,
            double minY, double minZ, double maxX, double maxY, double maxZ) {
        boolean vertical = minY < centerY + SLAM_HEIGHT && maxY > centerY - SLAM_BELOW;
        return vertical && horizontalDistanceToBox(centerX, centerZ, minX, minZ, maxX, maxZ) <= radius;
    }

    /** Fracao da recarga que ja passou (0 = acabou de usar, 1 = pronto). */
    public static float cooldownProgress(long remainingTicks, long totalTicks) {
        if (totalTicks <= 0 || remainingTicks <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, 1.0F - remainingTicks / (float) totalTicks));
    }
}
