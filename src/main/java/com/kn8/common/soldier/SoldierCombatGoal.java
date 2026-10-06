// src/main/java/com/kn8/common/soldier/SoldierCombatGoal.java
package com.kn8.common.soldier;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Combate do soldado: atirador mantem distancia ({@code keep_distance} do JSON, com folga) e atira com linha de
 * visada; quem usa lamina ou soco se aproxima ate o alcance (medido ate a borda da hitbox, para kaiju grandes).
 */
final class SoldierCombatGoal extends Goal {

    private static final double DISTANCE_SLACK = 4.0;
    private static final double SPEED = 1.0;
    private static final double RETREAT_SPEED = 1.1;
    private static final int REPATH_TICKS = 10;

    private final SoldierEntity soldier;
    private int repath;

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
    public void stop() {
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
        // Distancia ate a borda da hitbox do alvo (kaiju sao grandes).
        double edge = Math.sqrt(target.getBoundingBox().distanceToSqr(soldier.getEyePosition()));
        boolean sees = soldier.getSensing().hasLineOfSight(target);
        if (soldier.isShooter()) {
            double keep = soldier.keepDistance();
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                if (edge > keep + DISTANCE_SLACK || !sees) {
                    soldier.getNavigation().moveTo(target, SPEED);
                } else if (edge < keep - DISTANCE_SLACK) {
                    Vec3 away = soldier.position().subtract(target.position()).normalize().scale(keep);
                    Vec3 spot = soldier.position().add(away);
                    soldier.getNavigation().moveTo(spot.x, spot.y, spot.z, RETREAT_SPEED);
                } else {
                    soldier.getNavigation().stop();
                }
            }
            if (sees && edge <= soldier.attackReach()) {
                soldier.startAttack(target);
            }
            return;
        }
        if (edge > soldier.attackReach() * 0.9) {
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                soldier.getNavigation().moveTo(target, SPEED);
            }
        } else {
            soldier.getNavigation().stop();
            soldier.startAttack(target);
        }
    }
}
