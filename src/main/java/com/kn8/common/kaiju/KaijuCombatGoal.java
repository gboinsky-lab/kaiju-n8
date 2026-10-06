// src/main/java/com/kn8/common/kaiju/KaijuCombatGoal.java
package com.kn8.common.kaiju;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Combate do kaiju (M8): persegue o alvo e, ao alcance, inicia uma habilidade do JSON (ou o ataque basico de
 * especies sem habilidades). Durante uma habilidade o kaiju para e encara o alvo; o dano sai no tick de impacto,
 * no servidor ({@link KaijuEntity#tickAbility}), nunca pela animacao.
 *
 * <p>Aproximacao em duas fases (correcao do M8): longe, segue o caminho da navegacao; perto e ainda fora do
 * alcance, anda direto ate o alvo pelo controle de movimento, porque a navegacao encerra o caminho antes de
 * encostar (tolerancia de waypoint de mobs largos) e o kaiju ficava parado a ~2,4 blocos sem morder.</p>
 */
final class KaijuCombatGoal extends Goal {

    private static final int PATH_RECALC_TICKS = 10;
    /** Abaixo desta distancia entre as bordas (alem do alcance), o kaiju anda direto em vez de seguir o caminho. */
    private static final double DIRECT_APPROACH_DISTANCE = 3.0;
    private static final float LOOK_SPEED = 30.0F;

    private final KaijuEntity kaiju;
    private final double speedModifier;
    private int recalcCooldown;

    KaijuCombatGoal(KaijuEntity kaiju, double speedModifier) {
        this.kaiju = kaiju;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = kaiju.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        recalcCooldown = 0;
    }

    @Override
    public void stop() {
        kaiju.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = kaiju.getTarget();
        if (target == null) {
            return;
        }
        kaiju.getLookControl().setLookAt(target, LOOK_SPEED, LOOK_SPEED);
        if (kaiju.isUsingAbility()) {
            kaiju.getNavigation().stop();
            return;
        }
        double edge = kaiju.edgeDistance(target);
        double reach = kaiju.approachReach();
        if (edge > reach && (kaiju.isLarge() || edge <= reach + DIRECT_APPROACH_DISTANCE)) {
            // Perto, ou kaiju grande (a navegacao vanilla falha com mobs largos): anda direto ate o alvo. O que for
            // fragil no caminho e quebrado pelo DestructionService (Parte 3), se a destruicao estiver ligada.
            kaiju.getNavigation().stop();
            kaiju.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speedModifier);
            kaiju.clearPathIfBlocked();
        } else if (edge > reach && --recalcCooldown <= 0) {
            recalcCooldown = PATH_RECALC_TICKS;
            kaiju.getNavigation().moveTo(target, speedModifier);
        }
        kaiju.tryAttack(target);
    }
}
