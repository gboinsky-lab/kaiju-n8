// src/main/java/com/kn8/common/soldier/special/HoshinaCombatGoal.java
package com.kn8.common.soldier.special;

import java.util.EnumSet;
import java.util.Optional;

import com.kn8.common.data.def.SpecialSoldierDef;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Combate do Hoshina (0.6-D), na ordem de prioridade do texto do Miguel: aproximar rapido (dash para a frente com o
 * alvo longe), atacar com as tecnicas do perfil (a de maior prioridade pronta e ao alcance) e, sem tecnica pronta,
 * golpes basicos com a espada. Esquiva, Kaeshi-uchi e parry sao reacoes da propria entidade (HoshinaEntity#react).
 *
 * <p>Atiradores com {@code ranged} no perfil (Mina, Reno; Miguel, 2026-10-15) nao param no alcance da arma: recuam
 * se o alvo chega perto, correm em volta dele na faixa de distancia para distrai-lo e perseguem se ele se afasta,
 * atirando em movimento ({@link #rangedTick}).</p>
 */
final class HoshinaCombatGoal extends Goal {

    private static final double SPEED = 1.15;
    private static final int REPATH_TICKS = 8;
    /** Folga em volta da distancia de manter, em blocos (sem ficar indo e voltando na borda). */
    private static final double KEEP_SLACK = 3.0;
    /** Quanto a distancia precisa crescer entre duas reavaliacoes para contar como "o alvo esta se afastando". */
    private static final double LEAVING_DELTA = 0.6;
    /** Passo do giro em volta do alvo a cada reavaliacao (radianos, ~35 graus). */
    private static final double ORBIT_STEP = 0.6;

    private final HoshinaEntity hoshina;
    private int repath;
    private double lastEdge = -1.0;
    private int orbitSide = 1;
    private int orbitTicks;

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
        Optional<SpecialSoldierDef.Ranged> ranged = hoshina.profile().flatMap(SpecialSoldierDef::ranged);
        if (ranged.isPresent()) {
            rangedTick(target, ranged.get());
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

    /** Atirador: recua, circula ou persegue pela distancia (borda a borda) e atira em movimento. */
    private void rangedTick(LivingEntity target, SpecialSoldierDef.Ranged ranged) {
        double edge = hoshina.edgeTo(target);
        boolean sees = hoshina.getSensing().hasLineOfSight(target);
        if (sees && edge <= hoshina.attackReach()) {
            hoshina.startAttack(target);
        }
        if (--repath > 0) {
            return;
        }
        repath = REPATH_TICKS;
        boolean leaving = lastEdge >= 0.0 && edge - lastEdge > LEAVING_DELTA;
        lastEdge = edge;
        double keep = ranged.keepDistance();
        if (!sees || edge > keep + KEEP_SLACK || (leaving && edge > ranged.retreatDistance())) {
            // Longe, sem visao ou o alvo indo embora: persegue (com o dash do perfil se estiver muito longe).
            hoshina.gapClose(target);
            hoshina.getNavigation().moveTo(target, ranged.chaseSpeed());
            return;
        }
        if (edge < ranged.retreatDistance()) {
            // Perto demais: recua na direcao oposta ao alvo, com um pouco de giro para nao correr em linha reta.
            if (!moveAround(target, keep, ORBIT_STEP * 0.5, ranged.retreatSpeed())) {
                orbitSide = -orbitSide;
                moveAround(target, keep, ORBIT_STEP * 0.5, ranged.retreatSpeed());
            }
            return;
        }
        if (!ranged.orbit()) {
            hoshina.getNavigation().stop();
            return;
        }
        orbitTicks += REPATH_TICKS;
        if (orbitTicks >= ranged.orbitSwitchTicks()) {
            orbitTicks = 0;
            orbitSide = -orbitSide;
        }
        // Na faixa de distancia: corre em volta do alvo (distrai e nao fica parado na frente dele).
        if (!moveAround(target, keep, ORBIT_STEP, ranged.orbitSpeed())) {
            orbitSide = -orbitSide;
            orbitTicks = 0;
            moveAround(target, keep, ORBIT_STEP, ranged.orbitSpeed());
        }
    }

    /**
     * Vai para o ponto a {@code keep} blocos da borda do alvo, girado {@code step} radianos em volta dele no lado
     * {@link #orbitSide}. Falso se nao ha caminho.
     */
    private boolean moveAround(LivingEntity target, double keep, double step, double speed) {
        Vec3 center = target.position();
        Vec3 from = hoshina.position().subtract(center);
        double angle = Math.atan2(from.z, from.x) + step * orbitSide;
        double radius = keep + target.getBbWidth() * 0.5;
        Vec3 spot = center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
        return hoshina.getNavigation().moveTo(spot.x, hoshina.getY(), spot.z, speed);
    }
}
