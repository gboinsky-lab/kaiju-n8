// src/main/java/com/kn8/common/soldier/SoldierTargetGoal.java
package com.kn8.common.soldier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.kn8.common.career.ExamTargets;
import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.host.KaijuFormService;
import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.training.SparringService;
import com.kn8.core.soldier.SoldierTargeting;
import com.kn8.core.soldier.SquadRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
        if (held instanceof Player player && !soldier.canAttack(player)) {
            // T-4: o hospedeiro voltou a forma humana (ou subiu de patente): larga na hora, sem esperar a avaliacao.
            soldier.setTarget(null);
            held = null;
        }
        if (SparringService.partnerOf(soldier) != null) {
            // TREINO: no treino de combate so o parceiro (o servico encerra por tempo, distancia ou kaiju perto).
            soldier.setTarget(SparringService.tickSoldier(soldier));
            return;
        }
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
        List<Mob> found = new ArrayList<>(soldier.level().getEntitiesOfClass(KaijuEntity.class,
                soldier.getBoundingBox().inflate(range), kaiju -> kaiju.isAlive() && soldier.canAttack(kaiju)
                        && examAllowed(kaiju)
                        && (home == null || kaiju.getUUID().equals(assigned)
                        || kaiju.getBoundingBox().distanceToSqr(home) <= leash * leash)));
        List<LivingEntity> hosts = hostiles(range, home, leash);
        List<SoldierTargeting.Candidate> candidates = new ArrayList<>(found.size() + hosts.size());
        int currentId = -1;
        for (int i = 0; i < found.size() + hosts.size(); i++) {
            LivingEntity enemy = i < found.size() ? found.get(i) : hosts.get(i - found.size());
            if (enemy == current) {
                currentId = i;
            }
            double distance = guard ? Math.sqrt(enemy.getBoundingBox().distanceToSqr(home)) : soldier.distanceTo(enemy);
            boolean big = enemy instanceof KaijuEntity kaiju
                    ? kaiju.def().map(def -> def.kaijuClass() != KaijuClass.YOJU).orElse(false) : true;
            candidates.add(new SoldierTargeting.Candidate(i, distance,
                    threatensReloadingAlly(enemy), revenge && attacker == enemy, threatensAlly(enemy), big,
                    enemy.getUUID().equals(assigned)));
        }
        int chosen = SoldierTargeting.choose(params, candidates, currentId);
        if (chosen >= 0) {
            LivingEntity pick = chosen < found.size() ? found.get(chosen) : hosts.get(chosen - found.size());
            if (pick != current) {
                AiTuning.logTarget(soldier, current, pick, pick instanceof Player ? "hospedeiro transformado"
                        : pick.getUUID().equals(assigned) ? "kaiju designado"
                        : "melhor pontuacao entre " + candidates.size());
                soldier.setTarget(pick);
            }
        } else if (revenge && attacker.isAlive() && soldier.canAttack(attacker)) {
            // Sem kaiju por perto, revida quem o feriu (como o HurtByTargetGoal de antes: outro mob hostil).
            if (attacker != current) {
                AiTuning.logTarget(soldier, current, attacker, "revide");
                soldier.setTarget(attacker);
            }
        } else if (current != null && (current instanceof KaijuEntity || current instanceof Player
                || !current.isAlive())) {
            soldier.setTarget(null);
        }
    }

    /**
     * T-4: o hospedeiro transformado que a Forca de Defesa trata como kaiju ({@link KaijuFormService#hostileToForce})
     * entra na escolha como um kaiju grande, com a mesma coleira do esquadrao. O proprio esquadrao dele tambem
     * [SUPOSICAO: a decisao do Miguel fala so da patente].
     */
    private List<LivingEntity> hostiles(double range, Vec3 home, double leash) {
        List<LivingEntity> hosts = new ArrayList<>();
        for (Player player : soldier.level().getEntitiesOfClass(Player.class, soldier.getBoundingBox().inflate(range),
                player -> player.isAlive() && KaijuFormService.hostileToForce(player) && soldier.canAttack(player))) {
            if (home == null || player.getBoundingBox().distanceToSqr(home) <= leash * leash) {
                hosts.add(player);
            }
        }
        return hosts;
    }

    /**
     * EXAME: kaiju de prova e do candidato (o exame nao e ganho pelos soldados); so o esquadrao do dono da prova e os
     * candidatos que fazem a prova com ele lutam com eles.
     */
    private boolean examAllowed(KaijuEntity kaiju) {
        if (!ExamTargets.isExam(kaiju)) {
            return true;
        }
        UUID owner = ExamTargets.owner(kaiju);
        return owner != null && (soldier.squadLeader().filter(owner::equals).isPresent()
                || owner.equals(ExamTargets.candidateOwner(soldier)));
    }

    /** IA-3 (protecao de aliados): o alvo do kaiju e outro soldado ou um jogador. */
    private boolean threatensAlly(LivingEntity enemy) {
        LivingEntity target = enemy instanceof Mob mob ? mob.getTarget() : null;
        return target != null && target != soldier && (target instanceof SoldierEntity || target instanceof Player
                || target instanceof CivilianEntity);
    }

    /** O alvo do kaiju e outro soldado recarregando (a cobertura e a funcao de quem esta livre). */
    private boolean threatensReloadingAlly(LivingEntity enemy) {
        return enemy instanceof Mob mob && mob.getTarget() instanceof SoldierEntity ally && ally != soldier
                && ally.isReloading();
    }
}
