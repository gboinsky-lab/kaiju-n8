// src/main/java/com/kn8/common/kaiju/KaijuCombatGoal.java
package com.kn8.common.kaiju;

import java.util.EnumSet;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuAiDef;
import com.kn8.core.kaiju.KaijuTactics;
import com.kn8.core.kaiju.KaijuTargeting;

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
 *
 * <p>IA-1A: checa o progresso a cada {@code [kaijuAi] progressCheckTicks}. Sem chegar mais perto nem ferir o alvo
 * (ex.: embaixo de um soldado no alto de um pilar), pula e recalcula o caminho; sem progresso por {@code giveUpTicks}, marca
 * o alvo como inalcancavel e o {@link KaijuTargetGoal} escolhe outro (antes ficava parado embaixo para sempre).</p>
 *
 * <p>IA-4: perfil da especie ({@code kaiju_ai/<id>.json}): janela parada depois de golpe pesado, circular o alvo,
 * recuar com pouca vida, chegar espalhado em grupo, velocidade de perseguicao e de giro.</p>
 */
final class KaijuCombatGoal extends Goal {

    private static final int PATH_RECALC_TICKS = 10;
    /** Abaixo desta distancia entre as bordas (alem do alcance), o kaiju anda direto em vez de seguir o caminho. */
    private static final double DIRECT_APPROACH_DISTANCE = 3.0;
    /** IA-4: passo do giro em volta do alvo ao circular (radianos, ~35 graus). */
    private static final double CIRCLE_STEP = 0.6;

    private final KaijuEntity kaiju;
    private final double speedModifier;
    private int recalcCooldown;
    private long nextProgressCheck;
    private double lastEdge;
    private int stuckTicks;
    private LivingEntity lastTarget;
    /** IA-4: perfil da especie (kaiju_ai): janela depois do golpe pesado, circular, recuar. */
    private boolean wasUsingAbility;
    private long recoveryUntil;
    private long circleUntil;
    private long retreatUntil;
    private long retreatReadyAt;
    private int circleSide = 1;
    private long neighbourCheckAt;
    private boolean neighbour;

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
        resetProgress();
    }

    private void resetProgress() {
        LivingEntity target = kaiju.getTarget();
        lastEdge = target != null ? kaiju.edgeDistance(target) : 0.0;
        nextProgressCheck = kaiju.level().getGameTime() + ServerConfig.AI_PROGRESS_CHECK_TICKS.get();
        stuckTicks = 0;
    }

    @Override
    public void stop() {
        kaiju.getNavigation().stop();
        lastTarget = null;
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
        if (target != lastTarget) {
            // Trocou de alvo (KaijuTargetGoal): o progresso conta do zero para o novo.
            lastTarget = target;
            resetProgress();
        }
        KaijuAiDef ai = kaiju.aiDef();
        // IA-4: grandes giram devagar (look_speed), sem virar na hora para cada soldado.
        kaiju.getLookControl().setLookAt(target, ai.lookSpeed(), ai.lookSpeed());
        if (kaiju.isUsingAbility()) {
            // Sem zerar o progresso: golpe que erra (alvo no alto de um pilar) nao conta; o que acerta conta pelo
            // dano no alvo (checkProgress).
            kaiju.getNavigation().stop();
            wasUsingAbility = true;
            return;
        }
        long now = kaiju.level().getGameTime();
        if (wasUsingAbility) {
            wasUsingAbility = false;
            afterAbility(ai, now);
        }
        if (tactics(ai, target, now)) {
            resetProgress();
            return;
        }
        double edge = kaiju.edgeDistance(target);
        double reach = kaiju.approachReach();
        if (checkProgress(target, edge)) {
            return;
        }
        double speed = speedModifier * ai.chaseSpeed();
        double[] spot = approachSpot(ai, target, reach);
        if (edge > reach && (kaiju.isLarge() || edge <= reach + DIRECT_APPROACH_DISTANCE)) {
            // Perto, ou kaiju grande (a navegacao vanilla falha com mobs largos): anda direto ate o alvo. O que for
            // fragil no caminho e quebrado pelo DestructionService (Parte 3), se a destruicao estiver ligada.
            kaiju.getNavigation().stop();
            kaiju.getMoveControl().setWantedPosition(spot[0], target.getY(), spot[1], speed);
            kaiju.clearPathIfBlocked();
        } else if (edge > reach && --recalcCooldown <= 0) {
            recalcCooldown = PATH_RECALC_TICKS;
            kaiju.getNavigation().moveTo(spot[0], target.getY(), spot[1], speed);
        }
        kaiju.tryAttack(target);
    }

    /**
     * Fim de uma habilidade: depois de golpe pesado, janela parada ({@code recovery_after_heavy_ticks}, Honju e
     * No. 10); senao, chance de circular ({@code circle_chance}, Yoju).
     */
    private void afterAbility(KaijuAiDef ai, long now) {
        if (kaiju.lastAbilityWasHeavy() && ai.recoveryAfterHeavyTicks() > 0) {
            recoveryUntil = now + ai.recoveryAfterHeavyTicks();
        } else if (ai.circleChance() > 0.0F && kaiju.getRandom().nextFloat() < ai.circleChance()) {
            circleUntil = now + ai.circleTicks();
            circleSide = kaiju.getRandom().nextBoolean() ? 1 : -1;
        }
    }

    /** Janela, recuo e circular (perfil da especie). Devolve true se o tick ja foi usado. */
    private boolean tactics(KaijuAiDef ai, LivingEntity target, long now) {
        if (now < recoveryUntil) {
            // Janela de reacao: parado, sem golpe.
            kaiju.getNavigation().stop();
            kaiju.getMoveControl().setWantedPosition(kaiju.getX(), kaiju.getY(), kaiju.getZ(), 0.0);
            return true;
        }
        if (ai.retreatHealth() > 0.0F && now >= retreatReadyAt
                && kaiju.getHealth() < kaiju.getMaxHealth() * ai.retreatHealth()) {
            retreatUntil = now + ai.retreatTicks();
            retreatReadyAt = now + ai.retreatCooldownTicks();
        }
        if (now < retreatUntil) {
            double radius = ai.retreatDistance() + kaiju.getBbWidth() * 0.5;
            double[] spot = KaijuTactics.orbit(target.getX(), target.getZ(), kaiju.getX(), kaiju.getZ(), radius,
                    CIRCLE_STEP * 0.3, circleSide);
            moveTo(spot, target.getY(), speedModifier * 1.2);
            return true;
        }
        if (now < circleUntil) {
            double radius = kaiju.approachReach() + ai.circleDistance() + (kaiju.getBbWidth()
                    + target.getBbWidth()) * 0.5;
            double[] spot = KaijuTactics.orbit(target.getX(), target.getZ(), kaiju.getX(), kaiju.getZ(), radius,
                    CIRCLE_STEP, circleSide);
            moveTo(spot, target.getY(), speedModifier);
            return true;
        }
        return false;
    }

    /**
     * Ponto de chegada: o alvo; com outro kaiju a {@code separation_radius} (grupo), um ponto em volta do alvo no
     * angulo deste kaiju (cada um chega por um lado, sem empilhar).
     */
    private double[] approachSpot(KaijuAiDef ai, LivingEntity target, double reach) {
        if (ai.separationRadius() <= 0.0F || !hasNeighbour(ai)) {
            return new double[] {target.getX(), target.getZ()};
        }
        double radius = Math.max(0.5, reach * 0.5) + (kaiju.getBbWidth() + target.getBbWidth()) * 0.5;
        return KaijuTactics.around(target.getX(), target.getZ(), KaijuTactics.spreadAngle(kaiju.getId()), radius);
    }

    /** IA-7: ha outro kaiju perto? Busca refeita a cada {@code [kaijuAi] scanIntervalTicks} (antes, todo tick). */
    private boolean hasNeighbour(KaijuAiDef ai) {
        long now = kaiju.level().getGameTime();
        if (now >= neighbourCheckAt) {
            neighbourCheckAt = now + ServerConfig.AI_SCAN_INTERVAL.get();
            neighbour = !kaiju.level().getEntitiesOfClass(KaijuEntity.class,
                    kaiju.getBoundingBox().inflate(ai.separationRadius()), other -> other != kaiju && other.isAlive())
                    .isEmpty();
        }
        return neighbour;
    }

    private void moveTo(double[] spot, double y, double speed) {
        if (kaiju.isLarge()) {
            kaiju.getNavigation().stop();
            kaiju.getMoveControl().setWantedPosition(spot[0], y, spot[1], speed);
            kaiju.clearPathIfBlocked();
        } else if (--recalcCooldown <= 0) {
            recalcCooldown = PATH_RECALC_TICKS;
            kaiju.getNavigation().moveTo(spot[0], y, spot[1], speed);
        }
    }

    /** Devolve true se desistiu do alvo (inalcancavel). */
    private boolean checkProgress(LivingEntity target, double edge) {
        long now = kaiju.level().getGameTime();
        if (now < nextProgressCheck) {
            return false;
        }
        int interval = ServerConfig.AI_PROGRESS_CHECK_TICKS.get();
        nextProgressCheck = now + interval;
        boolean hit = target.getLastHurtByMob() == kaiju
                && target.tickCount - target.getLastHurtByMobTimestamp() <= interval;
        boolean progress = KaijuTargeting.madeProgress(lastEdge, edge, ServerConfig.AI_MIN_PROGRESS.get(), hit);
        lastEdge = edge;
        if (progress) {
            stuckTicks = 0;
            return false;
        }
        stuckTicks += interval;
        if (stuckTicks >= ServerConfig.AI_GIVE_UP_TICKS.get()) {
            kaiju.markUnreachable(target);
            return true;
        }
        // Tentativa de destravar: pula (degrau, borda), quebra o que esta no caminho ou embaixo (Miguel 2026-10-16:
        // kaiju nao fica preso no meio de construcoes) e recalcula o caminho ja no proximo tick.
        kaiju.getJumpControl().jump();
        kaiju.breakOutTowards(target);
        recalcCooldown = 0;
        return false;
    }
}
