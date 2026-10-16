// src/main/java/com/kn8/common/soldier/SoldierCombatGoal.java
package com.kn8.common.soldier;

import java.util.EnumSet;

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
 */
final class SoldierCombatGoal extends Goal {

    private static final double DISTANCE_SLACK = 4.0;
    private static final double SPEED = 1.0;
    private static final double RETREAT_SPEED = 1.1;
    private static final int REPATH_TICKS = 10;
    /** IA-5: a esta distancia da borda do alvo, o soldado de perto passa a chegar pelo proprio angulo. */
    private static final double SPREAD_RANGE = 8.0;

    private final SoldierEntity soldier;
    private final StuckWatch stuck = new StuckWatch();
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
        if (stuck.busy()) {
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
                soldier.startAttack(target);
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

    /** Recua na direcao oposta ao alvo. */
    private void retreat(LivingEntity target, double distance) {
        // IA-5: cada atirador recua por uma linha propria (desvio pelo id), sem empilhar no mesmo ponto.
        Vec3 away = soldier.position().subtract(target.position()).normalize().yRot((float) soldier.spreadOffset())
                .scale(distance);
        Vec3 spot = soldier.position().add(away);
        soldier.getNavigation().moveTo(spot.x, spot.y, spot.z, RETREAT_SPEED);
    }
}
