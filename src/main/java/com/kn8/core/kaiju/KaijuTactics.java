// src/main/java/com/kn8/core/kaiju/KaijuTactics.java
package com.kn8.core.kaiju;

/**
 * Contas da tatica por especie (IA-4) em Java puro: ponto de chegada espalhado (cercar sem empilhar) e ponto de
 * circular em volta do alvo.
 */
public final class KaijuTactics {

    /** Angulo de ouro (radianos): ids seguidos caem bem espalhados em volta do alvo. */
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private KaijuTactics() {
    }

    /** Angulo de chegada de cada kaiju (pelo id da entidade): cada um vem de um lado. */
    public static double spreadAngle(int entityId) {
        double angle = (entityId * GOLDEN_ANGLE) % (Math.PI * 2.0);
        return angle < 0 ? angle + Math.PI * 2.0 : angle;
    }

    /** Ponto a {@code radius} do alvo no angulo dado (x, z). */
    public static double[] around(double targetX, double targetZ, double angle, double radius) {
        return new double[] {targetX + Math.cos(angle) * radius, targetZ + Math.sin(angle) * radius};
    }

    /**
     * Proximo ponto ao circular: a {@code radius} do alvo, {@code step} radianos alem de onde o kaiju esta, no lado
     * {@code side} (1 ou -1).
     */
    public static double[] orbit(double targetX, double targetZ, double fromX, double fromZ, double radius,
            double step, int side) {
        double angle = Math.atan2(fromZ - targetZ, fromX - targetX) + step * side;
        return around(targetX, targetZ, angle, radius);
    }
}
