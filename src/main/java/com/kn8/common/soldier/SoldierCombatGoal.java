// src/main/java/com/kn8/common/soldier/SoldierCombatGoal.java
package com.kn8.common.soldier;

import java.util.EnumSet;
import java.util.Optional;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Combate do soldado: atirador mantem distancia ({@code keep_distance} do JSON, com folga) e atira com linha de
 * visada; quem usa lamina ou soco se aproxima ate o alcance (medido ate a borda da hitbox, para kaiju grandes).
 * 0.4: o atirador com arma de apoio troca para ela quando o kaiju chega perto (SoldierEntity#updateLoadout).
 * IA-1B: recarregando, o atirador recua ({@code [soldierAi] reloadRetreat}); sem visao do alvo, da a volta ate ter.
 * IA-5: separacao entre aliados (chegada e recuo por angulos proprios).
 * MOV-3: atirador com cobertura ({@link ShooterCover}). MOV-4: sai da area de golpe pesado ({@link DangerAvoid}) e
 * nao atira com aliado na frente.
 */
final class SoldierCombatGoal extends Goal {

    private static final double DISTANCE_SLACK = 4.0;
    private static final double SPEED = 1.0;
    private static final double RETREAT_SPEED = 1.1;
    private static final int REPATH_TICKS = 10;
    /** IA-5: a esta distancia da borda do alvo, o soldado de perto passa a chegar pelo proprio angulo. */
    private static final double SPREAD_RANGE = 8.0;
    /** MOV-4: intervalo da conferida da area de golpe pesado e tempo do passo que abre a linha de tiro. */
    private static final int DANGER_CHECK_TICKS = 2;
    private static final int SIDESTEP_TICKS = 20;

    private final SoldierEntity soldier;
    private final StuckWatch stuck = new StuckWatch();
    /** MOV-3: cobertura do atirador. */
    private final ShooterCover cover = new ShooterCover();
    /** MOV-4: saida da area de golpe pesado (a cada DANGER_CHECK_TICKS) e passo para abrir a linha de tiro. */
    private Vec3 evade;
    private int dangerCheck;
    private long sidestepUntil;
    private int sidestepSide = 1;
    private int repath;
    /** MOV-1: o tick quis andar (ate o alvo, contornando ou recuando): conta para o travamento. */
    private boolean wantsToMove;

    SoldierCombatGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = soldier.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        // Sincronizado com o cliente: liga a pose de mira (controller "arms" do SoldierEntity).
        soldier.setAggressive(true);
    }

    @Override
    public void stop() {
        cover.clear(soldier);
        soldier.restorePrimary();
        soldier.setAggressive(false);
        soldier.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = soldier.getTarget();
        if (target == null) {
            return;
        }
        soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);
        // MOV-1: saindo de um travamento, a navegacao e do StuckWatch ate chegar ao lado.
        if (stuck.busy(soldier)) {
            return;
        }
        // MOV-4: dentro da area de um golpe pesado em preparo (o aviso no chao), sai dela antes de qualquer coisa.
        if (--dangerCheck <= 0) {
            dangerCheck = DANGER_CHECK_TICKS;
            evade = DangerAvoid.exitSpot(soldier).orElse(null);
            if (evade != null) {
                AiTuning.logMove(soldier, "saindo da area do golpe pesado");
            }
        }
        if (evade != null) {
            wantsToMove = true;
            soldier.getNavigation().moveTo(evade.x, evade.y, evade.z,
                    ServerConfig.SOLDIER_DANGER_ESCAPE_SPEED.get());
            return;
        }
        if (stuck.tick(soldier, wantsToMove, target.position())) {
            return;
        }
        // Distancia ate a borda da hitbox do alvo (kaiju sao grandes).
        double edge = Math.sqrt(target.getBoundingBox().distanceToSqr(soldier.getEyePosition()));
        boolean sees = soldier.getSensing().hasLineOfSight(target);
        soldier.updateLoadout(edge);
        if (soldier.isShooter()) {
            // MOV-3: contra kaiju com ataque a distancia (e para recarregar) usa cobertura quando acha uma.
            if (cover.tick(soldier, target, edge, sees)) {
                wantsToMove = cover.moving();
                return;
            }
            double keep = soldier.keepDistance();
            if (soldier.isReloading()) {
                // IA-1B (Miguel: "recuar e recarregar"): recarregando, recua ate a distancia de manter + o recuo.
                keep += ServerConfig.SOLDIER_RELOAD_RETREAT.get();
                if (--repath <= 0) {
                    repath = REPATH_TICKS;
                    wantsToMove = edge < keep;
                    if (wantsToMove) {
                        retreat(target, keep);
                    }
                }
                return;
            }
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                if (edge > keep + DISTANCE_SLACK || !sees) {
                    soldier.getNavigation().moveTo(target, SPEED);
                    wantsToMove = true;
                } else if (edge < keep - DISTANCE_SLACK) {
                    retreat(target, keep);
                    wantsToMove = true;
                } else {
                    soldier.getNavigation().stop();
                    wantsToMove = false;
                }
            }
            if (sees && edge <= soldier.attackReach()) {
                shoot(target);
            }
            return;
        }
        if (edge > soldier.attackReach() * 0.9) {
            wantsToMove = true;
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                // IA-5: perto, cada um chega por um lado (sem empilhar no mesmo ponto); longe, o caminho ate o alvo.
                if (edge < SPREAD_RANGE) {
                    Vec3 spot = soldier.approachSpot(target, soldier.attackReach() * 0.6);
                    soldier.getNavigation().moveTo(spot.x, spot.y, spot.z, SPEED);
                } else {
                    soldier.getNavigation().moveTo(target, SPEED);
                }
            }
        } else {
            wantsToMove = false;
            soldier.getNavigation().stop();
            soldier.startAttack(target);
        }
    }

    /**
     * MOV-4: atira, salvo com um aliado na linha de tiro: ai segura o tiro e da {@code [soldierAi] allyLineSidestep}
     * blocos para o lado (alternando o lado a cada vez).
     */
    void shoot(LivingEntity target) {
        long now = soldier.level().getGameTime();
        if (now < sidestepUntil) {
            return;
        }
        double step = ServerConfig.SOLDIER_ALLY_LINE_SIDESTEP.get();
        if (step > 0.0 && DangerAvoid.allyInLine(soldier, target)) {
            Vec3 to = target.position().subtract(soldier.position());
            Vec3 side = new Vec3(-to.z, 0.0, to.x).normalize().scale(step * sidestepSide);
            sidestepSide = -sidestepSide;
            Optional<Vec3> spot = MoveSafety.standable(soldier, soldier.position().add(side));
            if (spot.isPresent()) {
                soldier.getNavigation().moveTo(spot.get().x, spot.get().y, spot.get().z, SPEED);
                sidestepUntil = now + SIDESTEP_TICKS;
                AiTuning.logMove(soldier, "aliado na linha de tiro, passo para o lado");
                return;
            }
        }
        soldier.startAttack(target);
    }

    /** Recua na direcao oposta ao alvo. */
    private void retreat(LivingEntity target, double distance) {
        // IA-5: cada atirador recua por uma linha propria (desvio pelo id), sem empilhar no mesmo ponto.
        Vec3 away = soldier.position().subtract(target.position()).normalize().yRot((float) soldier.spreadOffset())
                .scale(distance);
        Vec3 spot = soldier.position().add(away);
        soldier.getNavigation().moveTo(spot.x, spot.y, spot.z, RETREAT_SPEED);
    }
}
