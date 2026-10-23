// src/main/java/com/kn8/common/soldier/SquadMoveGoal.java
package com.kn8.common.soldier;

import java.util.EnumSet;

import com.kn8.common.config.ServerConfig;
import com.kn8.core.kaiju.KaijuTactics;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Movimento do esquadrao sem alvo (IA-5, bugs 13 e 14 da Fase 0): segue o lider (seguir, atacar e recuar) ou volta ao
 * posto ("ficar" e esquadroes de invasao, que antes vagavam). Cada membro para num ponto proprio em volta do lider
 * ou do posto ({@link KaijuTactics#spreadAngle}), sem empilhar. Prioridade acima do passeio: o soldado nao se
 * afasta passeando.
 */
public final class SquadMoveGoal extends Goal {

    private static final double SPEED = 1.0;
    private static final double RETREAT_SPEED = 1.25;
    private static final int REPATH_TICKS = 10;
    /** Raio do ponto de cada membro em volta do posto (blocos). */
    private static final double POST_SPREAD = 1.5;
    /** PM-6: na guarda, volta ao ponto do anel quando passa desta distancia dele (blocos). */
    private static final double GUARD_SPOT_SLACK = 2.5;

    private final SoldierEntity soldier;
    private int repath;

    public SquadMoveGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return ServerConfig.SPEC.isLoaded() && soldier.getTarget() == null && tooFar(1.0);
    }

    @Override
    public boolean canContinueToUse() {
        // Chega mais perto do que a distancia que dispara (sem ficar parando e voltando na borda).
        return soldier.getTarget() == null && tooFar(0.5);
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        soldier.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--repath > 0) {
            return;
        }
        repath = REPATH_TICKS;
        Vec3 home = soldier.squadHome();
        if (home == null) {
            return;
        }
        double[] spot = spot(home);
        double speed = soldier.squadOrder() == SquadRules.Order.RETREAT ? RETREAT_SPEED : SPEED;
        soldier.getNavigation().moveTo(spot[0], home.y, spot[1], speed);
    }

    /**
     * Ponto deste membro (x, z) em volta do lider ou do posto. PM-6: na guarda, num anel pela funcao (atirador mais
     * perto do centro, assalto mais para fora), cada um num angulo proprio: a area fica cercada, nao amontoada.
     */
    private double[] spot(Vec3 home) {
        double radius;
        if (soldier.squadOrder() == SquadRules.Order.GUARD) {
            radius = ServerConfig.SQUAD_PROTECT_RADIUS.get() * soldier.squadRole().guardRing();
        } else {
            radius = soldier.squadPost() != null ? POST_SPREAD : ServerConfig.SQUAD_FOLLOW_DISTANCE.get() * 0.4;
        }
        return KaijuTactics.around(home.x, home.z, KaijuTactics.spreadAngle(soldier.getId()), radius);
    }

    /**
     * Mais longe do ponto do esquadrao que a distancia de seguir (lider) ou o raio de guarda (posto) x fator; na
     * guarda (PM-6), longe do proprio ponto no anel.
     */
    private boolean tooFar(double factor) {
        Vec3 home = soldier.squadHome();
        if (home == null) {
            return false;
        }
        if (soldier.squadOrder() == SquadRules.Order.GUARD) {
            double[] spot = spot(home);
            double dx = soldier.getX() - spot[0];
            double dz = soldier.getZ() - spot[1];
            double limit = GUARD_SPOT_SLACK * factor;
            return dx * dx + dz * dz > limit * limit;
        }
        double limit = (soldier.squadPost() != null ? ServerConfig.SQUAD_GUARD_RADIUS.get()
                : ServerConfig.SQUAD_FOLLOW_DISTANCE.get()) * factor;
        return soldier.position().distanceToSqr(home) > limit * limit;
    }
}
