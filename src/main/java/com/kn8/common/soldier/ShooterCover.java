package com.kn8.common.soldier;

import java.util.Optional;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Cobertura do atirador comum (MOV-3, plano de mobilidade v3 §5), um por soldado dentro do goal de combate. Contra
 * kaiju com ataque a distancia o atirador fica no ponto de atirar ao lado da cobertura e se esconde atras dela quando
 * o kaiju prepara um ataque contra ele (por {@code [soldierAi] coverHideTicks}) e para recarregar; contra qualquer
 * kaiju, recarrega atras de uma cobertura se houver. A cada {@code coverCheckTicks} confere se a cobertura ainda corta
 * a visao (kaiju contornou, bloco quebrado), se o kaiju chegou perto demais ou se o ponto ficou na area de um golpe
 * pesado; se nao, procura outra (sem cobertura, o goal volta a manter distancia como antes).
 */
final class ShooterCover {

    private static final double SPEED = 1.2;
    /** Chegou ao ponto (blocos, no plano). */
    private static final double ARRIVED = 0.6;

    private CoverFinder.Cover cover;
    private long nextCheck;
    private long hideUntil;
    private boolean moving;

    /** O tick andou ate a cobertura ou o ponto de atirar (conta para o travamento). */
    boolean moving() {
        return moving;
    }

    /**
     * Um tick do atirador: verdadeiro se a cobertura cuidou do movimento (e do tiro) neste tick; falso para o goal
     * seguir como antes.
     */
    boolean tick(SoldierEntity soldier, LivingEntity target, double edge, boolean sees) {
        moving = false;
        boolean reloading = soldier.isReloading();
        boolean ranged = target instanceof KaijuEntity kaiju && kaiju.hasRangedAttack();
        if (!ServerConfig.SOLDIER_COVER.get() || (!ranged && !reloading)) {
            clear(soldier);
            return false;
        }
        long now = soldier.level().getGameTime();
        if (now >= nextCheck) {
            nextCheck = now + ServerConfig.SOLDIER_COVER_CHECK_TICKS.get();
            if (cover != null && !CoverFinder.stillHides(soldier, target, cover.spot())) {
                AiTuning.logMove(soldier, "cobertura perdida (contornada, quebrada ou perto demais)");
                clear(soldier);
            }
            // Ainda esconde mas o ponto de tiro nao ve mais o kaiju: fica com ela (para recarregar) ate achar uma com
            // ponto de tiro, sem trocar a cada conferida.
            boolean blind = cover != null && cover.peek().map(peek -> !CoverFinder.canShootFrom(soldier, target,
                    peek)).orElse(true);
            if (cover == null || blind) {
                Optional<CoverFinder.Cover> found = CoverFinder.find(soldier, target, soldier.keepDistance());
                if (blind && found.isPresent() && found.get().peek().isEmpty()) {
                    found = Optional.empty();
                }
                if (blind && found.isEmpty()) {
                    cover = new CoverFinder.Cover(cover.spot(), Optional.empty());
                }
                if (found.isPresent()) {
                    cover = found.get();
                    soldier.setCoverSpot(cover.spot());
                    AiTuning.logMove(soldier, "cobertura em " + fmt(cover.spot())
                            + (cover.peek().isPresent() ? " com ponto de tiro" : " sem ponto de tiro"));
                }
            }
        }
        if (cover == null || (!reloading && cover.peek().isEmpty())) {
            return false;
        }
        // O kaiju prepara um ataque contra este soldado: abaixa atras da cobertura.
        if (target instanceof KaijuEntity kaiju && kaiju.abilityTarget().orElse(null) == soldier) {
            hideUntil = now + ServerConfig.SOLDIER_COVER_HIDE_TICKS.get();
        }
        boolean hide = reloading || now < hideUntil || cover.peek().isEmpty();
        Vec3 goal = hide ? cover.spot() : cover.peek().get();
        if (Math.hypot(goal.x - soldier.getX(), goal.z - soldier.getZ()) > ARRIVED) {
            moving = true;
            soldier.getNavigation().moveTo(goal.x, goal.y, goal.z, SPEED);
        } else {
            soldier.getNavigation().stop();
        }
        if (!hide && sees && edge <= soldier.attackReach() && !DangerAvoid.allyInLine(soldier, target)) {
            soldier.startAttack(target);
        }
        return true;
    }

    void clear(SoldierEntity soldier) {
        cover = null;
        soldier.setCoverSpot(null);
    }

    private static String fmt(Vec3 v) {
        return String.format("%.0f, %.0f, %.0f", v.x, v.y, v.z);
    }
}
