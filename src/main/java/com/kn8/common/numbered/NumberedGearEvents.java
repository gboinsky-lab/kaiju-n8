package com.kn8.common.numbered;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * PM-11B: ganchos do servidor para os equipamentos numerados do jogador: tick (cauda do Numbers 10, asas do
 * Numbers 4) e guarda da cauda no dano recebido. A guarda roda depois do bloqueio/aparar do combate (prioridade
 * baixa): golpe ja anulado nao gasta a guarda.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class NumberedGearEvents {

    private NumberedGearEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            NumberedGearService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        event.setAmount(NumberedGearService.guard(player, event.getSource(), event.getAmount()));
    }
}
