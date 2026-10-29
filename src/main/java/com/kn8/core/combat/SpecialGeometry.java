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
     * PM-1: o alvo esta na faixa do corte em avanco? Faixa reta no plano a partir de (startX, startZ) na direcao
     * (dirX, dirZ) unitaria, de comprimento {@code length} e meia largura {@code halfWidth}, contra a caixa inteira
     * do alvo (o ponto da faixa mais perto do centro da caixa), na mesma faixa de altura do golpe no chao.
     */
    public static boolean inLunge(double startX, double startY, double startZ, double dirX, double dirZ,
            double length, double halfWidth, double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ) {
        boolean vertical = minY < startY + SLAM_HEIGHT && maxY > startY - SLAM_BELOW;
        if (!vertical) {
            return false;
        }
        double centerX = (minX + maxX) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
        double along = Math.max(0.0, Math.min(length, (centerX - startX) * dirX + (centerZ - startZ) * dirZ));
        double pointX = startX + dirX * along;
        double pointZ = startZ + dirZ * along;
        return horizontalDistanceToBox(pointX, pointZ, minX, minZ, maxX, maxZ) <= halfWidth;
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

    /** Gravidade e freios do ar do vanilla (entidade viva: vy = (vy - 0,08) x 0,98; horizontal x 0,91 por tick). */
    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG_Y = 0.98;
    private static final double AIR_DRAG_XZ = 0.91;

    /** COMBATE-3: velocidade horizontal que anda {@code distance} blocos em {@code ticks} no ar (com o freio). */
    public static double leapHorizontalSpeed(double distance, int ticks) {
        double travelled = (1.0 - Math.pow(AIR_DRAG_XZ, ticks)) / (1.0 - AIR_DRAG_XZ);
        return distance / travelled;
    }

    /** COMBATE-3: velocidade vertical do salto que volta a altura de partida em {@code ticks} (gravidade e freio). */
    public static double leapVerticalSpeed(int ticks) {
        // A altura no tick T e linear na velocidade inicial: y = v x A - B (A e B pela simulacao do vanilla).
        double withOne = leapHeight(1.0, ticks);
        double withZero = leapHeight(0.0, ticks);
        return -withZero / (withOne - withZero);
    }

    /** Altura (blocos) depois de {@code ticks} saindo com a velocidade vertical {@code speed}. */
    public static double leapHeight(double speed, int ticks) {
        double y = 0.0;
        double v = speed;
        for (int i = 0; i < ticks; i++) {
            y += v;
            v = (v - GRAVITY) * AIR_DRAG_Y;
        }
        return y;
    }

    /** Fracao da recarga que ja passou (0 = acabou de usar, 1 = pronto). */
    public static float cooldownProgress(long remainingTicks, long totalTicks) {
        if (totalTicks <= 0 || remainingTicks <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, 1.0F - remainingTicks / (float) totalTicks));
    }
}
