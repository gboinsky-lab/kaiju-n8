// src/main/java/com/kn8/common/site/SiteEvents.java
package com.kn8.common.site;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Liga os locais da obra (C3) aos eventos do jogo (game bus, servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class SiteEvents {

    private SiteEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ServerConfig.SPEC.isLoaded()) {
            SiteService.tick(player);
        }
    }

    /** Ao sair, voltar ou trocar de dimensao o nome do local aparece de novo na proxima entrada. */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SiteService.forget(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SiteService.forget(player);
        }
    }
}
