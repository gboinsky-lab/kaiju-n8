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

    /**
     * PM-4 (flanco): angulo, em volta do alvo, do ponto atras dele. {@code targetYawDegrees} e o yaw do Minecraft
     * (olhar = (-sen, cos)); atras = o oposto do olhar.
     */
    public static double behindAngle(float targetYawDegrees) {
        double yaw = Math.toRadians(targetYawDegrees);
        return Math.atan2(-Math.cos(yaw), Math.sin(yaw));
    }

    /**
     * PM-4 (limite de atacantes): espera a vez quem ainda nao esta colado no alvo quando ja ha {@code maxAttackers}
     * outros colados; 0 = sem limite. Quem ja esta colado nunca espera (nao larga o golpe no meio).
     */
    public static boolean mustWait(int engagedOthers, boolean selfEngaged, int maxAttackers) {
        return maxAttackers > 0 && !selfEngaged && engagedOthers >= maxAttackers;
    }

    /**
     * PM-4 (apoio a distancia): -1 = se afasta (alvo mais perto que {@code keep - band}), 0 = fica onde esta,
     * 1 = se aproxima (alvo alem de {@code keep}).
     */
    public static int supportMove(double edge, double keep, double band) {
        if (edge < keep - band) {
            return -1;
        }
        return edge > keep ? 1 : 0;
    }
}
