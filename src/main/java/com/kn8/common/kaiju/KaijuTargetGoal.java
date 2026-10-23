// src/main/java/com/kn8/common/kaiju/KaijuTargetGoal.java
package com.kn8.common.kaiju;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.kn8.common.combat.WeaponIndex;
import com.kn8.common.ai.AiTuning;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.WeaponDef;
import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.core.kaiju.KaijuTargeting;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Escolha de alvo do kaiju (IA-1A), no lugar do {@code HurtByTargetGoal} + {@code NearestAttackableTargetGoal}
 * vanilla: reavalia a cada {@code [kaijuAi] retargetTicks} quem o kaiju percebe e quem ataca
 * ({@link KaijuTargeting}). Comum ataca o mais perto (todos em volta apanham, sem perseguir um so para sempre);
 * inteligente elimina atiradores e quem mais o feriu. Alvo dado por numerado vale por {@code commandTicks}. O alvo
 * atual sempre concorre, mesmo que nao seja jogador/soldado (dado por comando ou outro sistema), enquanto percebido.
 */
final class KaijuTargetGoal extends Goal {

    private final KaijuEntity kaiju;
    private long nextEvaluation;

    KaijuTargetGoal(KaijuEntity kaiju) {
        this.kaiju = kaiju;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        return ServerConfig.SPEC.isLoaded() && !kaiju.isRetreating();
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
        long now = kaiju.level().getGameTime();
        LivingEntity current = kaiju.getTarget();
        if (current != null && valid(current, now)) {
            kaiju.aiMemory().see(current.position(), now);
        }
        // IA-7: alvo morto ou removido reavalia na hora (longe dos jogadores o intervalo e maior).
        if ((now < nextEvaluation && (current == null || current.isAlive())) || kaiju.isUsingAbility()) {
            return;
        }
        nextEvaluation = now + AiTuning.retargetInterval(kaiju, ServerConfig.AI_RETARGET_TICKS.get());
        int memory = ServerConfig.AI_THREAT_MEMORY_TICKS.get();
        kaiju.aiMemory().prune(now, memory);
        if (current != null && kaiju.aiMemory().commanded(now) && valid(current, now)) {
            return;
        }
        double range = kaiju.getAttributeValue(Attributes.FOLLOW_RANGE);
        KaijuTargeting.Params params = new KaijuTargeting.Params(range, ServerConfig.AI_HEARING_FRACTION.get(),
                kaiju.isSmart(), ServerConfig.AI_SHOOTER_BONUS.get(), ServerConfig.AI_THREAT_PER_DAMAGE.get(),
                ServerConfig.AI_MAX_THREAT_BONUS.get(), ServerConfig.AI_SWITCH_MARGIN.get());
        List<LivingEntity> found = kaiju.level().getEntitiesOfClass(LivingEntity.class,
                kaiju.getBoundingBox().inflate(range), entity -> valid(entity, now) && (entity == current
                        || isEnemy(entity) || kaiju.aiMemory().recentAttacker(entity.getUUID(), now, memory)));
        List<KaijuTargeting.Candidate> candidates = new ArrayList<>(found.size());
        int currentId = -1;
        for (int i = 0; i < found.size(); i++) {
            LivingEntity entity = found.get(i);
            if (entity == current) {
                currentId = i;
            }
            candidates.add(new KaijuTargeting.Candidate(i, kaiju.distanceTo(entity),
                    kaiju.getSensing().hasLineOfSight(entity), isShooter(entity),
                    kaiju.aiMemory().recentDamage(entity.getUUID(), now, memory),
                    kaiju.aiMemory().recentAttacker(entity.getUUID(), now, memory)));
        }
        int chosen = KaijuTargeting.choose(params, candidates, currentId);
        LivingEntity next = chosen < 0 ? null : found.get(chosen);
        if (next != current) {
            AiTuning.logTarget(kaiju, current, next, next == null ? "ninguem percebido"
                    : "melhor pontuacao entre " + found.size());
            kaiju.setTarget(next);
        }
    }

    /** Vivo, atacavel e nao marcado como inalcancavel (kaiju e partes nunca: o comando do numerado decide). */
    private boolean valid(LivingEntity entity, long now) {
        return entity != kaiju && entity.isAlive() && !(entity instanceof KaijuEntity) && kaiju.canAttack(entity)
                && !kaiju.aiMemory().unreachable(entity.getUUID(), now);
    }

    private static boolean isEnemy(LivingEntity entity) {
        return entity instanceof SoldierEntity || entity instanceof Player player && !player.isCreative()
                && !player.isSpectator()
                || entity instanceof CivilianEntity && ServerConfig.EVAC_KAIJU_TARGET_CIVILIANS.get();
    }

    /** Funcao do inimigo: atirador (soldado ou jogador com arma de fogo na mao). */
    static boolean isShooter(LivingEntity entity) {
        if (entity instanceof SoldierEntity soldier) {
            return soldier.isShooter();
        }
        return entity instanceof Player player && WeaponIndex.find(player.getMainHandItem(), false)
                .map(weapon -> weapon.style() == WeaponDef.Style.FIREARM).orElse(false);
    }
}
