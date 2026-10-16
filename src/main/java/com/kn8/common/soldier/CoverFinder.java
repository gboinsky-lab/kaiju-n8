package com.kn8.common.soldier;

import java.util.List;
import java.util.Optional;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.soldier.CoverScore;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Cobertura do atirador no mundo (MOV-3, plano de mobilidade v3 §5). Amostra pontos em aneis em volta do soldado
 * (raio {@code [soldierAi] coverRadius}), fica com os que tem chao ({@link MoveSafety#standable}) e onde um bloco
 * corta a visao do kaiju ate os olhos e o peito do soldado (o mesmo teste de colisao dos tiros: nada de ver ou atirar
 * atraves de parede); para cada um procura, a um passo para o lado, um ponto com linha de tiro (sair para atirar). A
 * conta fica no {@link CoverScore}. Custo limitado: so atirador em combate, a cada {@code coverCheckTicks}.
 */
public final class CoverFinder {

    /** Raios dos aneis (fracao do raio maximo) e direcoes por anel. */
    private static final double[] RINGS = {0.3, 0.55, 0.8, 1.0};
    private static final int DIRECTIONS = 12;
    /** Passo para o lado ate o ponto de atirar (blocos). */
    private static final double PEEK_STEP = 1.5;
    /** Altura do "peito" conferida alem dos olhos (fracao da altura dos olhos). */
    private static final double CHEST = 0.6;
    /** Raio em que outro soldado ja ocupa o ponto. */
    private static final double CROWD_RADIUS = 1.5;

    private CoverFinder() {
    }

    /** Ponto de cobertura e o ponto ao lado para atirar (vazio se nao ha linha de tiro dali). */
    public record Cover(Vec3 spot, Optional<Vec3> peek) {
    }

    /** Melhor cobertura contra {@code threat}, ou vazio. */
    public static Optional<Cover> find(SoldierEntity soldier, LivingEntity threat, double preferredDistance) {
        double radius = ServerConfig.SOLDIER_COVER_RADIUS.get();
        double minThreat = ServerConfig.SOLDIER_COVER_MIN_THREAT_DISTANCE.get();
        List<SoldierEntity> allies = soldier.level().getEntitiesOfClass(SoldierEntity.class,
                soldier.getBoundingBox().inflate(radius + CROWD_RADIUS), other -> other != soldier && other.isAlive());
        int count = RINGS.length * DIRECTIONS;
        double[] scores = new double[count];
        Cover[] covers = new Cover[count];
        int i = 0;
        for (double ring : RINGS) {
            for (int d = 0; d < DIRECTIONS; d++, i++) {
                scores[i] = CoverScore.REJECTED;
                double angle = Math.PI * 2.0 * d / DIRECTIONS;
                Vec3 wanted = soldier.position().add(Math.cos(angle) * radius * ring, 0.0,
                        Math.sin(angle) * radius * ring);
                Optional<Vec3> spot = MoveSafety.standable(soldier, wanted);
                if (spot.isEmpty()) {
                    continue;
                }
                double strength = hidingStrength(soldier, threat, spot.get());
                if (strength < 0.0) {
                    continue;
                }
                Optional<Vec3> peek = peekFrom(soldier, threat, spot.get());
                int crowd = 0;
                for (SoldierEntity ally : allies) {
                    if (ally.position().distanceTo(spot.get()) < CROWD_RADIUS || (ally.coverSpot() != null
                            && ally.coverSpot().distanceTo(spot.get()) < CROWD_RADIUS)) {
                        crowd++;
                    }
                }
                CoverScore.Candidate candidate = new CoverScore.Candidate(true, peek.isPresent(),
                        edge(threat, spot.get()), soldier.position().distanceTo(spot.get()), crowd, strength,
                        dangerous(soldier, spot.get()));
                scores[i] = CoverScore.score(candidate, preferredDistance, minThreat);
                covers[i] = new Cover(spot.get(), peek);
            }
        }
        int best = CoverScore.best(scores);
        return best < 0 ? Optional.empty() : Optional.of(covers[best]);
    }

    /** A cobertura ainda serve: corta a visao, nao foi contornada nem destruida, kaiju longe e fora de perigo. */
    public static boolean stillHides(SoldierEntity soldier, LivingEntity threat, Vec3 spot) {
        return hidingStrength(soldier, threat, spot) >= 0.0
                && edge(threat, spot) >= ServerConfig.SOLDIER_COVER_MIN_THREAT_DISTANCE.get()
                && !dangerous(soldier, spot);
    }

    /** O ponto de atirar ainda ve o kaiju. */
    public static boolean canShootFrom(SoldierEntity soldier, LivingEntity threat, Vec3 peek) {
        return clear(soldier, peek.add(0.0, soldier.getEyeHeight(), 0.0), threat.getBoundingBox().getCenter());
    }

    /**
     * Resistencia do bloco que corta a visao do kaiju ate os olhos e o peito do soldado em {@code spot}; -1 se o
     * kaiju ve um dos dois (nao esconde).
     */
    private static double hidingStrength(SoldierEntity soldier, LivingEntity threat, Vec3 spot) {
        Vec3 from = threat.getEyePosition();
        double strength = Double.MAX_VALUE;
        for (double height : new double[] {soldier.getEyeHeight(), soldier.getEyeHeight() * CHEST}) {
            BlockHitResult hit = soldier.level().clip(new ClipContext(from, spot.add(0.0, height, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, threat));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return -1.0;
            }
            strength = Math.min(strength, soldier.level().getBlockState(hit.getBlockPos()).getBlock()
                    .getExplosionResistance());
        }
        return strength;
    }

    /** Ponto a um passo para o lado (em relacao ao kaiju) com chao e linha de tiro. */
    private static Optional<Vec3> peekFrom(SoldierEntity soldier, LivingEntity threat, Vec3 spot) {
        Vec3 toThreat = new Vec3(threat.getX() - spot.x, 0.0, threat.getZ() - spot.z);
        if (toThreat.lengthSqr() < 1.0E-4) {
            return Optional.empty();
        }
        toThreat = toThreat.normalize();
        Vec3 side = new Vec3(-toThreat.z, 0.0, toThreat.x);
        for (int s : new int[] {1, -1}) {
            Optional<Vec3> peek = MoveSafety.standable(soldier, spot.add(side.scale(PEEK_STEP * s)));
            if (peek.isPresent() && Math.abs(peek.get().y - spot.y) <= 1.0 && canShootFrom(soldier, threat,
                    peek.get())) {
                return peek;
            }
        }
        return Optional.empty();
    }

    /** Dentro da area de um golpe pesado sendo preparado por um kaiju por perto. */
    private static boolean dangerous(SoldierEntity soldier, Vec3 spot) {
        double radius = ServerConfig.SOLDIER_COVER_DANGER_RADIUS.get();
        AABB around = new AABB(spot, spot).inflate(radius + 16.0);
        for (KaijuEntity kaiju : soldier.level().getEntitiesOfClass(KaijuEntity.class, around,
                KaijuEntity::isPreparingHeavy)) {
            if (edge(kaiju, spot) < radius) {
                return true;
            }
        }
        return false;
    }

    private static double edge(LivingEntity entity, Vec3 spot) {
        return Math.sqrt(entity.getBoundingBox().distanceToSqr(spot));
    }

    private static boolean clear(SoldierEntity soldier, Vec3 from, Vec3 to) {
        return soldier.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                soldier)).getType() == HitResult.Type.MISS;
    }
}
