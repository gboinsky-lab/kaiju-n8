// src/main/java/com/kn8/common/network/NetworkEvents.java
package com.kn8.common.network;

import com.kn8.KN8Constants;
import com.kn8.common.server.KN8Server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Reenvio dos dados privados nos eventos de ciclo de vida do jogador e limpeza ao sair (game bus). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class NetworkEvents {

    private NetworkEvents() {
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkSync.sendAll(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkSync.sendAll(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NetworkSync.sendAll(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KN8Server services = KN8Server.getOrNull(player.getServer());
            if (services != null) {
                services.rateLimiter().forget(player.getUUID());
                services.privateSync().forget(player.getUUID());
            }
        }
    }
}
