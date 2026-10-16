// src/main/java/com/kn8/common/ai/AiTuning.java
package com.kn8.common.ai;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Ajustes de custo da IA (IA-7, Parte 13 do plano de IA), comuns a kaiju e soldados: escolha de alvo mais espacada
 * longe de todo jogador e log de decisoes opcional ({@code [kaijuAi] debugLog}). Sem estado proprio.
 */
public final class AiTuning {

    private AiTuning() {
    }

    /**
     * Intervalo da escolha de alvo: {@code base}; sem jogador vivo a {@code [kaijuAi] farFromPlayers} blocos, vezes
     * {@code farRetargetFactor} (a luta continua, so a troca de alvo fica mais lenta onde ninguem ve).
     */
    public static int retargetInterval(Mob mob, int base) {
        double far = ServerConfig.AI_FAR_FROM_PLAYERS.get();
        if (mob.level().hasNearbyAlivePlayer(mob.getX(), mob.getY(), mob.getZ(), far)) {
            return base;
        }
        return base * ServerConfig.AI_FAR_RETARGET_FACTOR.get();
    }

    /** Log de troca de alvo (so com {@code [kaijuAi] debugLog}). */
    public static void logTarget(Mob mob, LivingEntity from, LivingEntity to, String reason) {
        if (!ServerConfig.AI_DEBUG_LOG.get()) {
            return;
        }
        KN8Constants.LOGGER.info("[kn8 IA] {} #{} alvo {} -> {} ({})", mob.getType().toShortString(), mob.getId(),
                describe(from), describe(to), reason);
    }

    private static String describe(LivingEntity entity) {
        return entity == null ? "nenhum" : entity.getType().toShortString() + " #" + entity.getId();
    }
}
