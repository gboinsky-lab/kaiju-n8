// src/main/java/com/kn8/common/soldier/SoldierTargetGoal.java
package com.kn8.common.soldier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.soldier.SoldierTargeting;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Alvo dos soldados (IA-1B, comuns e especiais), no lugar do {@code NearestAttackableTargetGoal} com visao: Miguel,
 * "soldados devem detectar os kaiju independente de onde estejam". Todo kaiju a ate {@code [soldierAi]
 * detectionRange} blocos conta, vendo ou nao (o goal de combate da a volta ate ter linha de tiro). Escolha em
 * {@link SoldierTargeting}: mais perto, cobrindo o aliado que esta recarregando e revidando quem o feriu.
 */
public final class SoldierTargetGoal extends Goal {

    private final SoldierEntity soldier;
    private long nextEvaluation;

    public SoldierTargetGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return ServerConfig.SPEC.isLoaded();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        nextEvaluation = 0;
    }

    @Override
    public void tick() {
        long now = soldier.level().getGameTime();
        // IA-7: alvo morto ou removido reavalia na hora (longe dos jogadores o intervalo e maior e o soldado ficava
        // parado preso a um alvo morto).
        LivingEntity held = soldier.getTarget();
        if (soldier.duelOpponent().isPresent() && soldier.level() instanceof ServerLevel server) {
            // Duelo de teste: so o oponente marcado, sem kaiju nem esquadrao.
            Entity foe = server.getEntity(soldier.duelOpponent().get());
            soldier.setTarget(foe instanceof LivingEntity living && living.isAlive() ? living : null);
            return;
        }
        if (now < nextEvaluation && (held == null || held.isAlive())) {
            return;
        }
        nextEvaluation = now + AiTuning.retargetInterval(soldier, ServerConfig.SOLDIER_RETARGET_TICKS.get());
        if (soldier.squadOrder() == SquadRules.Order.RETREAT && soldier.squadLeader().isPresent()) {
            // IA-5: "recuar" larga a luta e volta para o lider (SquadMoveGoal).
            soldier.setTarget(null);
            return;
        }
        if (soldier.squadOrder() == SquadRules.Order.FOCUS && soldier.squadLeader().isPresent()
                && soldier.level() instanceof ServerLevel server) {
            // PM-6: "focar" so no kaiju designado pelo lider; caido ou fora do mundo, o esquadrao volta a seguir.
            Entity focus = soldier.assignedKaiju() == null ? null : server.getEntity(soldier.assignedKaiju());
            if (focus instanceof KaijuEntity kaiju && kaiju.isAlive()) {
                if (kaiju != held) {
                    AiTuning.logTarget(soldier, held, kaiju, "alvo designado pelo lider");
                    soldier.setTarget(kaiju);
                }
                return;
            }
            AiTuning.logMove(soldier, "alvo designado caiu: volta a seguir");
            SquadService.focusEnded(soldier);
            soldier.setTarget(null);
        }
        // IA-5: seguindo ou no posto, so luta perto do lider/posto; o kaiju designado (invasao) conta sempre.
        Vec3 home = soldier.isLeashed() ? soldier.squadHome() : null;
        double leash = soldier.leashRange();
        // PM-6: na guarda vale quem esta mais dentro da area (distancia ao centro), nao o mais perto do soldado.
        boolean guard = home != null && soldier.squadOrder() == SquadRules.Order.GUARD;
        UUID assigned = soldier.assignedKaiju();
        double range = ServerConfig.SOLDIER_DETECTION_RANGE.get();
        SpecialSoldierDef.Tactics tactics = soldier instanceof HoshinaEntity special
                ? special.profile().map(SpecialSoldierDef::tactics).orElse(SpecialSoldierDef.Tactics.DEFAULT)
                : SpecialSoldierDef.Tactics.DEFAULT;
        SoldierTargeting.Params params = new SoldierTargeting.Params(range, ServerConfig.SOLDIER_COVER_BONUS.get(),
                ServerConfig.SOLDIER_REVENGE_BONUS.get(), ServerConfig.SOLDIER_SWITCH_MARGIN.get(),
                tactics.protectAlly(), tactics.bigTarget(), ServerConfig.SQUAD_ASSIGNED_BONUS.get());
        LivingEntity current = soldier.getTarget();
        LivingEntity attacker = soldier.getLastHurtByMob();
        boolean revenge = attacker != null
                && soldier.tickCount - soldier.getLastHurtByMobTimestamp() <= ServerConfig.SOLDIER_REVENGE_TICKS.get();
        List<KaijuEntity> found = soldier.level().getEntitiesOfClass(KaijuEntity.class,
                soldier.getBoundingBox().inflate(range), kaiju -> kaiju.isAlive() && soldier.canAttack(kaiju)
                        && (home == null || kaiju.getUUID().equals(assigned)
                        || kaiju.getBoundingBox().distanceToSqr(home) <= leash * leash));
        List<SoldierTargeting.Candidate> candidates = new ArrayList<>(found.size());
        int currentId = -1;
        for (int i = 0; i < found.size(); i++) {
            KaijuEntity kaiju = found.get(i);
            if (kaiju == current) {
                currentId = i;
            }
            double distance = guard ? Math.sqrt(kaiju.getBoundingBox().distanceToSqr(home)) : soldier.distanceTo(kaiju);
            candidates.add(new SoldierTargeting.Candidate(i, distance,
                    threatensReloadingAlly(kaiju), revenge && attacker == kaiju, threatensAlly(kaiju),
                    kaiju.def().map(def -> def.kaijuClass() != KaijuClass.YOJU).orElse(false),
                    kaiju.getUUID().equals(assigned)));
        }
        int chosen = SoldierTargeting.choose(params, candidates, currentId);
        if (chosen >= 0) {
            if (found.get(chosen) != current) {
                AiTuning.logTarget(soldier, current, found.get(chosen), found.get(chosen).getUUID().equals(assigned)
                        ? "kaiju designado" : "melhor pontuacao entre " + found.size());
                soldier.setTarget(found.get(chosen));
            }
        } else if (revenge && attacker.isAlive() && soldier.canAttack(attacker)) {
            // Sem kaiju por perto, revida quem o feriu (como o HurtByTargetGoal de antes: outro mob hostil).
            if (attacker != current) {
                AiTuning.logTarget(soldier, current, attacker, "revide");
                soldier.setTarget(attacker);
            }
        } else if (current != null && (current instanceof KaijuEntity || !current.isAlive())) {
            soldier.setTarget(null);
        }
    }

    /** IA-3 (protecao de aliados): o alvo do kaiju e outro soldado ou um jogador. */
    private boolean threatensAlly(KaijuEntity kaiju) {
        LivingEntity target = kaiju.getTarget();
        return target != null && target != soldier && (target instanceof SoldierEntity || target instanceof Player);
    }

    /** O alvo do kaiju e outro soldado recarregando (a cobertura e a funcao de quem esta livre). */
    private boolean threatensReloadingAlly(KaijuEntity kaiju) {
        return kaiju.getTarget() instanceof SoldierEntity ally && ally != soldier && ally.isReloading();
    }
}
