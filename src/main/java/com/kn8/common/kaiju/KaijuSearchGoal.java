// src/main/java/com/kn8/common/kaiju/KaijuSearchGoal.java
package com.kn8.common.kaiju;

import java.util.EnumSet;
import java.util.Optional;

import com.kn8.common.config.ServerConfig;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Busca (IA-1A): perdeu o alvo de vista (o comum nao percebe mais), vai ate a ultima posicao conhecida e olha em
 * volta ate {@code [kaijuAi] searchTicks} depois da ultima vez que o viu. Encontrando alguem no caminho, o
 * {@link KaijuTargetGoal} da o alvo e o combate assume.
 */
final class KaijuSearchGoal extends Goal {

    private static final int PATH_RECALC_TICKS = 20;
    private static final double ARRIVED_DISTANCE = 2.0;
    private static final int LOOK_AROUND_TICKS = 20;

    private final KaijuEntity kaiju;
    private final double speedModifier;
    private int cooldown;

    KaijuSearchGoal(KaijuEntity kaiju, double speedModifier) {
        this.kaiju = kaiju;
        this.speedModifier = speedModifier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Optional<Vec3> spot() {
        if (!ServerConfig.SPEC.isLoaded() || kaiju.getTarget() != null || kaiju.isStaggered()) {
            return Optional.empty();
        }
        return kaiju.aiMemory().lastKnown(kaiju.level().getGameTime(), ServerConfig.AI_SEARCH_TICKS.get());
    }

    @Override
    public boolean canUse() {
        return spot().isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        cooldown = 0;
    }

    @Override
    public void stop() {
        kaiju.getNavigation().stop();
        if (kaiju.getTarget() == null) {
            kaiju.aiMemory().forgetLastKnown();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Optional<Vec3> spot = spot();
        if (spot.isEmpty()) {
            return;
        }
        Vec3 pos = spot.get();
        double dx = pos.x - kaiju.getX();
        double dz = pos.z - kaiju.getZ();
        boolean arrived = Math.sqrt(dx * dx + dz * dz) <= ARRIVED_DISTANCE + kaiju.getBbWidth() / 2.0;
        if (arrived) {
            kaiju.getNavigation().stop();
            if (--cooldown <= 0) {
                cooldown = LOOK_AROUND_TICKS;
                double angle = kaiju.getRandom().nextDouble() * Math.PI * 2.0;
                kaiju.getLookControl().setLookAt(kaiju.getX() + Math.cos(angle) * 8.0, kaiju.getEyeY(),
                        kaiju.getZ() + Math.sin(angle) * 8.0);
            }
            return;
        }
        kaiju.getLookControl().setLookAt(pos.x, pos.y + 1.0, pos.z);
        if (kaiju.isLarge()) {
            kaiju.getNavigation().stop();
            kaiju.getMoveControl().setWantedPosition(pos.x, pos.y, pos.z, speedModifier);
            kaiju.clearPathIfBlocked();
        } else if (--cooldown <= 0) {
            cooldown = PATH_RECALC_TICKS;
            kaiju.getNavigation().moveTo(pos.x, pos.y, pos.z, speedModifier);
        }
    }
}
