// src/main/java/com/kn8/common/parkour/ParkourEvents.java
package com.kn8.common.parkour;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga o parkour (0.5.0-E) aos eventos do jogo (game bus, so no servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class ParkourEvents {

    private ParkourEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            ParkourService.tick(player);
        }
    }

    /** Rolamento: corta o dano da queda pelo multiplicador do servidor. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()
                && event.getDistance() > 0.0F) {
            float multiplier = ParkourService.onFall(player, event.getDistance());
            if (multiplier < 1.0F) {
                event.setDamageMultiplier(event.getDamageMultiplier() * multiplier);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ParkourService.reset(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ParkourService.reset(player);
        }
    }
}
