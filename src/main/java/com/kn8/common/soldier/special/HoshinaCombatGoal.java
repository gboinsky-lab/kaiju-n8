// src/main/java/com/kn8/common/soldier/special/HoshinaCombatGoal.java
package com.kn8.common.soldier.special;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Combate do Hoshina (0.6-D), na ordem de prioridade do texto do Miguel: aproximar rapido (dash para a frente com o
 * alvo longe), atacar com as tecnicas do perfil (a de maior prioridade pronta e ao alcance) e, sem tecnica pronta,
 * golpes basicos com a espada. Esquiva, Kaeshi-uchi e parry sao reacoes da propria entidade (HoshinaEntity#react).
 */
final class HoshinaCombatGoal extends Goal {

    private static final double SPEED = 1.15;
    private static final int REPATH_TICKS = 8;

    private final HoshinaEntity hoshina;
    private int repath;

    HoshinaCombatGoal(HoshinaEntity hoshina) {
        this.hoshina = hoshina;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = hoshina.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        hoshina.setAggressive(true);
    }

    @Override
    public void stop() {
        hoshina.setAggressive(false);
        hoshina.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = hoshina.getTarget();
        if (target == null) {
            return;
        }
        hoshina.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (hoshina.isUsingTechnique()) {
            return;
        }
        Optional<String> technique = hoshina.chooseTechnique(target);
        if (technique.isPresent() && hoshina.startTechnique(technique.get(), target)) {
            return;
        }
        double edge = hoshina.edgeTo(target);
        if (edge > hoshina.attackReach() * 0.9) {
            hoshina.gapClose(target);
            if (--repath <= 0) {
                repath = REPATH_TICKS;
                hoshina.getNavigation().moveTo(target, SPEED);
            }
        } else {
            hoshina.getNavigation().stop();
            hoshina.startAttack(target);
        }
    }
}
