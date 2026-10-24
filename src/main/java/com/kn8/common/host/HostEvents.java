package com.kn8.common.host;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** T-1: sorteio da larva no tick do jogador e voo dela no tick da propria entidade (so no servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class HostEvents {

    private HostEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            LarvaService.playerTick(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof KaijuEntity kaiju && !kaiju.level().isClientSide()
                && LarvaService.isHostLarva(kaiju) && ServerConfig.SPEC.isLoaded()) {
            LarvaService.larvaTick(kaiju);
        }
    }
}
