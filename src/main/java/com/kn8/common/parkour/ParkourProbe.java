// src/main/java/com/kn8/common/parkour/ParkourProbe.java
package com.kn8.common.parkour;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Geometria do parkour (0.5.0-E): obstaculo a frente, parede ao lado e borda, pelas colisoes do mundo. Serve ao
 * cliente (detectar o que da para fazer) e ao servidor (conferir a intencao com folga para o atraso da rede).
 * {@code reach} e quanto a caixa do jogador e empurrada para o lado conferido.
 */
public final class ParkourProbe {

    /** Passo da procura do topo do obstaculo (1/16 de bloco, um pixel). */
    private static final double STEP = 0.0625;
    /** Encolhe a caixa em cima e embaixo ao procurar parede (o chao e o teto nao contam como parede). */
    private static final double WALL_DEFLATE_Y = 0.1;

    private ParkourProbe() {
    }

    /** Frente horizontal pelo yaw (yaw 0 = +Z, como no vanilla). */
    public static Vec3 forward(float yRot) {
        double radians = Math.toRadians(yRot);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    /** Direita horizontal pelo yaw. */
    public static Vec3 right(float yRot) {
        Vec3 forward = forward(yRot);
        return new Vec3(-forward.z, 0.0, forward.x);
    }

    /**
     * Altura do obstaculo a {@code reach} na direcao {@code dir}: 0 = livre, -1 = mais alto que {@code maxHeight} ou
     * sem espaco em cima (teto). Senao, o menor deslocamento para cima em que a caixa do jogador cabe la (o topo do
     * obstaculo acima dos pes), conferindo tambem que o jogador sobe ate essa altura sem bater a cabeca.
     */
    public static double obstacleHeight(Entity entity, Vec3 dir, double reach, double maxHeight) {
        AABB here = entity.getBoundingBox();
        AABB ahead = here.move(dir.x * reach, 0.0, dir.z * reach);
        if (entity.level().noCollision(entity, ahead)) {
            return 0.0;
        }
        for (double dy = STEP; dy <= maxHeight + 1.0E-6; dy += STEP) {
            if (!entity.level().noCollision(entity, here.move(0.0, dy, 0.0))) {
                return -1.0;
            }
            if (entity.level().noCollision(entity, ahead.move(0.0, dy, 0.0))) {
                return dy;
            }
        }
        return -1.0;
    }

    /** Ha parede (qualquer colisao) a {@code reach} na direcao {@code dir}, sem contar chao e teto. */
    public static boolean wallAt(Entity entity, Vec3 dir, double reach) {
        AABB box = entity.getBoundingBox().deflate(0.0, WALL_DEFLATE_Y, 0.0).move(dir.x * reach, 0.0, dir.z * reach);
        return !entity.level().noCollision(entity, box);
    }

    /** Lado da parede para o wall-run: -1 esquerda, +1 direita, 0 nenhuma; com as duas, vale {@code hint}. */
    public static int wallSide(Entity entity, double reach, int hint) {
        Vec3 right = right(entity.getYRot());
        boolean onRight = wallAt(entity, right, reach);
        boolean onLeft = wallAt(entity, right.scale(-1.0), reach);
        if (onRight && onLeft) {
            return hint >= 0 ? 1 : -1;
        }
        return onRight ? 1 : onLeft ? -1 : 0;
    }

    /** Normal de parede no eixo mais proximo (paredes de bloco sao retas em X ou em Z). */
    public static Vec3 axisAligned(Vec3 normal) {
        return Math.abs(normal.x) >= Math.abs(normal.z) ? new Vec3(Math.signum(normal.x), 0.0, 0.0)
                : new Vec3(0.0, 0.0, Math.signum(normal.z));
    }

    /** Direcao ao longo da parede de normal {@code normal}, no sentido de {@code forward}. */
    public static Vec3 alongWall(Vec3 forward, Vec3 normal) {
        Vec3 along = forward.subtract(normal.scale(forward.dot(normal)));
        return along.lengthSqr() < 1.0E-6 ? Vec3.ZERO : along.normalize();
    }
}
