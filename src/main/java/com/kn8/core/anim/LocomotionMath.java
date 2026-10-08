// src/main/java/com/kn8/core/anim/LocomotionMath.java
package com.kn8.core.anim;

/** Contas do perfil de locomocao (0.5.0-C), sem Minecraft para o JUnit. */
public final class LocomotionMath {

    /** Abaixo disso (blocos por tick) a criatura esta parada: a animacao (idle) toca normal. */
    public static final double STOPPED_BLOCKS_PER_TICK = 0.005;

    private LocomotionMath() {
    }

    /**
     * Velocidade da animacao de andar para os pes acompanharem o chao: em uma volta de {@code loopTicks} a criatura
     * anda {@code blocksPerTick * loopTicks}; a animacao foi feita para andar {@code stride} nessa volta.
     */
    public static double animationSpeed(double blocksPerTick, double loopTicks, double stride, double min,
            double max) {
        if (blocksPerTick < STOPPED_BLOCKS_PER_TICK || loopTicks <= 0 || stride <= 0) {
            return 1.0;
        }
        double speed = blocksPerTick * loopTicks / stride;
        return Math.max(min, Math.min(max, speed));
    }

    /**
     * Proximo passo na conta de distancia do vanilla ({@code Entity.moveDist} soma 0,6 por bloco andado).
     */
    public static float nextStep(float moveDist, float blocksPerStep) {
        return moveDist + blocksPerStep * 0.6F;
    }
}
