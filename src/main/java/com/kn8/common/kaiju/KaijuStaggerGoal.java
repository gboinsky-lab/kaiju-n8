// src/main/java/com/kn8/common/kaiju/KaijuStaggerGoal.java
package com.kn8.common.kaiju;

import java.util.EnumSet;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Atordoado (IA-1A, Miguel: "fica parado por alguns segundos e depois volta"): enquanto o atordoamento dura, este
 * goal toma movimento, olhar e pulo; o kaiju para no lugar (sem andar, perseguir nem passear). A habilidade em
 * andamento e cancelada no {@link KaijuEntity#tick}; ao acabar, o combate volta sozinho.
 */
final class KaijuStaggerGoal extends Goal {

    private final KaijuEntity kaiju;

    KaijuStaggerGoal(KaijuEntity kaiju) {
        this.kaiju = kaiju;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return kaiju.isStaggered();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        kaiju.stopTriggeredAnim("action", null);
    }

    @Override
    public void tick() {
        kaiju.getNavigation().stop();
        kaiju.getMoveControl().setWantedPosition(kaiju.getX(), kaiju.getY(), kaiju.getZ(), 0.0);
        kaiju.setZza(0.0F);
        kaiju.setXxa(0.0F);
    }
}
