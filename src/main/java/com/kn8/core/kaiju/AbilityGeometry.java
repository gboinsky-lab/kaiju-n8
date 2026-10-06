// src/main/java/com/kn8/core/kaiju/AbilityGeometry.java
package com.kn8.core.kaiju;

/**
 * Geometria das habilidades de area de kaiju em Java puro (M11a), testavel com JUnit.
 *
 * <p>Golpe no chao ({@code kn8:area_melee}, ex.: slam): o centro fica na frente do kaiju, na borda da hitbox
 * ({@code meia largura} a frente do centro). Atinge quem tem a BORDA da hitbox dentro do {@code radius} do JSON, no
 * plano horizontal, e esta na faixa de altura do kaiju (do chao ate a altura dele).</p>
 */
public final class AbilityGeometry {

    private AbilityGeometry() {
    }

    /** Centro do golpe: deslocamento a frente do centro do kaiju (meia largura da hitbox). */
    public static double slamCenterForward(double kaijuWidth) {
        return kaijuWidth / 2.0;
    }

    /**
     * O alvo esta na area do golpe?
     *
     * @param dx            distancia em X do centro do golpe ao centro do alvo
     * @param dz            distancia em Z do centro do golpe ao centro do alvo
     * @param targetBottom  altura dos pes do alvo menos a dos pes do kaiju
     * @param targetWidth   largura da hitbox do alvo
     * @param targetHeight  altura da hitbox do alvo
     * @param radius        {@code radius} da habilidade (JSON)
     * @param kaijuHeight   altura da hitbox do kaiju
     */
    public static boolean inSlamArea(double dx, double dz, double targetBottom, double targetWidth,
            double targetHeight, double radius, double kaijuHeight) {
        double edge = Math.sqrt(dx * dx + dz * dz) - targetWidth / 2.0;
        boolean vertical = targetBottom < kaijuHeight && targetBottom + targetHeight > 0;
        return edge <= radius && vertical;
    }
}
