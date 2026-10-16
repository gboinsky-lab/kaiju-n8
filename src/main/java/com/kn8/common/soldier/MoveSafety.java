// src/main/java/com/kn8/common/soldier/MoveSafety.java
package com.kn8.common.soldier;

import java.util.Optional;

import com.kn8.common.config.ServerConfig;
import com.kn8.core.soldier.LandingMath;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Geometria da mobilidade dos soldados (MOV-1, plano de mobilidade v3 §4: "saltos precisam mover a entidade de
 * verdade, com colisao e pouso validos"). Antes de um impulso (esquiva, investida, avanco da tecnica, destravar),
 * confere pelas colisoes do mundo onde a caixa do soldado termina: anda em linha reta subindo degrau de ate 1 bloco,
 * para na parede e procura o chao embaixo (queda de ate {@code [soldierAi] landingMaxDrop}). As decisoes ficam no
 * {@link LandingMath}.
 */
public final class MoveSafety {

    /** Passo do percurso conferido (blocos). */
    private static final double STEP = 0.5;
    /** Degrau que o impulso sobe sozinho (blocos): o impulso tem um pouco de altura. */
    private static final double STEP_UP = 1.0;
    /** Folga ao procurar o chao (a caixa desce um pouco alem de cada bloco inteiro). */
    private static final double SUPPORT_EPSILON = 0.1;
    /** Procura de chao em volta de um ponto (destravar): ate esta altura acima do ponto pedido. */
    private static final int STAND_ABOVE = 2;

    private MoveSafety() {
    }

    /** Distancia aproximada que um impulso de {@code speed} percorre ({@code [soldierAi] dashBlocksPerSpeed}). */
    public static double dashDistance(double speed) {
        return speed * ServerConfig.SOLDIER_DASH_BLOCKS_PER_SPEED.get();
    }

    /**
     * Onde termina um impulso horizontal na direcao {@code dir} por {@code distance} blocos, com chao embaixo; vazio
     * se a parede esta logo a frente, se nao ha chao ate a queda maxima ou se a direcao e nula.
     */
    public static Optional<Vec3> landing(Mob mob, Vec3 dir, double distance) {
        Vec3 flat = new Vec3(dir.x, 0.0, dir.z);
        if (flat.lengthSqr() < 1.0E-6 || distance <= 0.0) {
            return Optional.empty();
        }
        flat = flat.normalize();
        Vec3 base = mob.position();
        AABB box = mob.getBoundingBox();
        Vec3 last = base;
        double travelled = 0.0;
        double lift = 0.0;
        for (double s = STEP; s <= distance + 1.0E-6; s += STEP) {
            Vec3 p = base.add(flat.scale(s)).add(0.0, lift, 0.0);
            if (!mob.level().noCollision(mob, box.move(p.subtract(base)))) {
                Vec3 up = p.add(0.0, STEP_UP, 0.0);
                if (lift > 0.0 || !mob.level().noCollision(mob, box.move(up.subtract(base)))
                        || !mob.level().noCollision(mob, box.move(last.subtract(base)).move(0.0, STEP_UP, 0.0))) {
                    break;
                }
                lift = STEP_UP;
                p = up;
            }
            last = p;
            travelled = s;
        }
        int drop = dropBelow(mob, box.move(last.subtract(base)));
        if (!LandingMath.acceptable(travelled, distance, ServerConfig.SOLDIER_LANDING_MIN_FRACTION.get(), drop,
                ServerConfig.SOLDIER_LANDING_MAX_DROP.get())) {
            return Optional.empty();
        }
        return Optional.of(last.subtract(0.0, drop, 0.0));
    }

    /**
     * Direcao de esquiva com pouso valido, na ordem do {@link LandingMath#dodgeOrder}: para os lados (misturado com
     * um pouco de "para longe") e, por ultimo, para tras. Vazio se nenhuma cabe.
     */
    public static Optional<Vec3> dodgeDirection(Mob mob, Vec3 away, boolean preferRight, double distance) {
        Vec3 back = new Vec3(away.x, 0.0, away.z);
        if (back.lengthSqr() < 1.0E-6) {
            back = Vec3.directionFromRotation(0.0F, mob.getYRot()).scale(-1.0);
        }
        back = back.normalize();
        Vec3 right = new Vec3(-back.z, 0.0, back.x);
        for (int side : LandingMath.dodgeOrder(preferRight)) {
            Vec3 dir = side == 0 ? back : right.scale(side).add(back.scale(0.6)).normalize();
            if (landing(mob, dir, distance).isPresent()) {
                return Optional.of(dir);
            }
        }
        return Optional.empty();
    }

    /**
     * Ponto em pe perto de {@code spot} (destravar): a caixa cabe ali, de ate {@code STAND_ABOVE} blocos acima do
     * ponto ate a queda maxima abaixo, com chao. Vazio se nao ha lugar.
     */
    public static Optional<Vec3> standable(Mob mob, Vec3 spot) {
        AABB box = mob.getBoundingBox();
        Vec3 base = mob.position();
        for (int dy = STAND_ABOVE; dy >= -ServerConfig.SOLDIER_LANDING_MAX_DROP.get(); dy--) {
            Vec3 p = new Vec3(spot.x, Math.floor(spot.y) + dy, spot.z);
            AABB at = box.move(p.subtract(base));
            if (mob.level().noCollision(mob, at) && dropBelow(mob, at) == 0) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /** A caixa do soldado cabe em {@code p} (pes) e tem chao logo abaixo (MOV-2: pouso do salto). */
    public static boolean restsAt(Mob mob, Vec3 p) {
        AABB at = mob.getBoundingBox().move(p.subtract(mob.position()));
        return mob.level().noCollision(mob, at) && dropBelow(mob, at) == 0;
    }

    /** A caixa do soldado cabe em {@code p} (pes), sem conferir o chao (MOV-2: arco do salto). */
    public static boolean fits(Mob mob, Vec3 p) {
        return mob.level().noCollision(mob, mob.getBoundingBox().move(p.subtract(mob.position())));
    }

    /** Quantos blocos a caixa cai ate o chao (0 = chao logo abaixo; -1 = mais que a queda maxima). */
    private static int dropBelow(Mob mob, AABB box) {
        boolean[] supported = new boolean[ServerConfig.SOLDIER_LANDING_MAX_DROP.get() + 1];
        for (int i = 0; i < supported.length; i++) {
            supported[i] = !mob.level().noCollision(mob, box.move(0.0, -(i + SUPPORT_EPSILON), 0.0));
        }
        return LandingMath.firstSupport(supported);
    }
}
