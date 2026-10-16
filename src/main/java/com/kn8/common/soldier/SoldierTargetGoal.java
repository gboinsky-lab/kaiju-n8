// src/main/java/com/kn8/common/soldier/SoldierTargetGoal.java
package com.kn8.common.soldier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.core.soldier.SoldierTargeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

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
        if (now < nextEvaluation) {
            return;
        }
        nextEvaluation = now + ServerConfig.SOLDIER_RETARGET_TICKS.get();
        double range = ServerConfig.SOLDIER_DETECTION_RANGE.get();
        SpecialSoldierDef.Tactics tactics = soldier instanceof HoshinaEntity special
                ? special.profile().map(SpecialSoldierDef::tactics).orElse(SpecialSoldierDef.Tactics.DEFAULT)
                : SpecialSoldierDef.Tactics.DEFAULT;
        SoldierTargeting.Params params = new SoldierTargeting.Params(range, ServerConfig.SOLDIER_COVER_BONUS.get(),
                ServerConfig.SOLDIER_REVENGE_BONUS.get(), ServerConfig.SOLDIER_SWITCH_MARGIN.get(),
                tactics.protectAlly(), tactics.bigTarget());
        LivingEntity current = soldier.getTarget();
        LivingEntity attacker = soldier.getLastHurtByMob();
        boolean revenge = attacker != null
                && soldier.tickCount - soldier.getLastHurtByMobTimestamp() <= ServerConfig.SOLDIER_REVENGE_TICKS.get();
        List<KaijuEntity> found = soldier.level().getEntitiesOfClass(KaijuEntity.class,
                soldier.getBoundingBox().inflate(range), kaiju -> kaiju.isAlive() && soldier.canAttack(kaiju));
        List<SoldierTargeting.Candidate> candidates = new ArrayList<>(found.size());
        int currentId = -1;
        for (int i = 0; i < found.size(); i++) {
            KaijuEntity kaiju = found.get(i);
            if (kaiju == current) {
                currentId = i;
            }
            candidates.add(new SoldierTargeting.Candidate(i, soldier.distanceTo(kaiju),
                    threatensReloadingAlly(kaiju), revenge && attacker == kaiju, threatensAlly(kaiju),
                    kaiju.def().map(def -> def.kaijuClass() != KaijuClass.YOJU).orElse(false)));
        }
        int chosen = SoldierTargeting.choose(params, candidates, currentId);
        if (chosen >= 0) {
            if (found.get(chosen) != current) {
                soldier.setTarget(found.get(chosen));
            }
        } else if (revenge && attacker.isAlive() && soldier.canAttack(attacker)) {
            // Sem kaiju por perto, revida quem o feriu (como o HurtByTargetGoal de antes: outro mob hostil).
            if (attacker != current) {
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
