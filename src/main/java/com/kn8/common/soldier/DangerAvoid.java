package com.kn8.common.soldier;

import java.util.Optional;

import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.core.kaiju.DangerZone;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Leitura do campo de batalha dos soldados (MOV-4, plano de mobilidade v3 §6): sair da area do golpe pesado em
 * preparo (a mesma do aviso no chao, {@link KaijuEntity#dangerZone}) e golpe de perto so com caminho livre ate o alvo
 * (nada de acertar atraves de parede) e atirador sem aliado na frente do tiro.
 */
public final class DangerAvoid {

    /** Kaiju procurados em volta (blocos). */
    private static final double SEARCH = 32.0;
    /** Direcoes tentadas para sair: a mais curta e as vizinhas (graus). */
    private static final double[] TURNS = {0.0, 45.0, -45.0, 90.0, -90.0};
    /** Folga em volta do aliado na linha de tiro (blocos). */
    private static final double ALLY_INFLATE = 0.2;

    private DangerAvoid() {
    }

    /** O soldado esta dentro (com a margem do config) da area de um golpe pesado em preparo. */
    public static Optional<DangerZone> zoneAt(Mob mob) {
        if (!ServerConfig.SOLDIER_AVOID_DANGER.get()) {
            return Optional.empty();
        }
        double margin = ServerConfig.SOLDIER_DANGER_MARGIN.get();
        for (KaijuEntity kaiju : mob.level().getEntitiesOfClass(KaijuEntity.class,
                mob.getBoundingBox().inflate(SEARCH), KaijuEntity::isAlive)) {
            Optional<DangerZone> zone = kaiju.dangerZone().filter(z -> z.contains(mob.getX(), mob.getZ(), margin));
            if (zone.isPresent()) {
                return zone;
            }
        }
        return Optional.empty();
    }

    /**
     * Ponto fora da area, com chao, para onde andar: o caminho mais curto e, se nao der, as direcoes vizinhas.
     * Vazio se o soldado esta fora de toda area ou nao ha para onde ir.
     */
    public static Optional<Vec3> exitSpot(Mob mob) {
        Optional<DangerZone> found = zoneAt(mob);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        DangerZone zone = found.get();
        double margin = ServerConfig.SOLDIER_DANGER_MARGIN.get();
        Vec3 look = Vec3.directionFromRotation(0.0F, mob.getYRot());
        double[] exit = zone.exit(mob.getX(), mob.getZ(), -look.x, -look.z);
        double distance = zone.exitDistance(mob.getX(), mob.getZ(), margin);
        for (double turn : TURNS) {
            Vec3 dir = new Vec3(exit[0], 0.0, exit[1]).yRot((float) Math.toRadians(turn));
            double step = distance / Math.max(0.3, Math.cos(Math.toRadians(turn)));
            Optional<Vec3> spot = MoveSafety.standable(mob, mob.position().add(dir.scale(step)));
            if (spot.isPresent() && !zone.contains(spot.get().x, spot.get().z, margin * 0.5)) {
                return spot;
            }
        }
        return Optional.empty();
    }

    /**
     * Caminho livre (sem bloco solido) dos olhos de {@code from} ate o ponto mais perto da caixa do alvo, ou ate o
     * centro dela: golpe de perto e tecnica corpo a corpo so com isso.
     */
    public static boolean clearReach(Mob from, LivingEntity target) {
        Vec3 eye = from.getEyePosition();
        return clear(from, eye, SoldierEntity.nearestInside(target.getBoundingBox(), eye))
                || clear(from, eye, target.getBoundingBox().getCenter());
    }

    /**
     * Um aliado (soldado ou jogador) esta na linha de tiro dos olhos do atirador ate o centro do alvo. O tiro passa
     * por aliados sem feri-los, mas atirar atraves deles nao e o que um soldado faz: o atirador se reposiciona.
     */
    public static boolean allyInLine(SoldierEntity shooter, LivingEntity target) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter();
        double length = eye.distanceTo(aim);
        AABB around = new AABB(eye, aim).inflate(ALLY_INFLATE);
        for (LivingEntity other : shooter.level().getEntitiesOfClass(LivingEntity.class, around,
                e -> e != shooter && e != target && e.isAlive() && (e instanceof SoldierEntity || e instanceof Player
                        || e instanceof CivilianEntity))) {
            if (other instanceof Player player && player.isSpectator()) {
                continue;
            }
            Optional<Vec3> hit = other.getBoundingBox().inflate(ALLY_INFLATE).clip(eye, aim);
            if (hit.isPresent() && hit.get().distanceTo(eye) < length) {
                return true;
            }
        }
        return false;
    }

    private static boolean clear(Mob from, Vec3 a, Vec3 b) {
        return from.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from))
                .getType() == HitResult.Type.MISS;
    }
}
