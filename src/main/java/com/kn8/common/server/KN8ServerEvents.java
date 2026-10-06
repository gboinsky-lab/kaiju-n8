// src/main/java/com/kn8/common/server/KN8ServerEvents.java
package com.kn8.common.server;

import com.kn8.KN8Constants;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Liga o ciclo de vida do {@link KN8Server} aos eventos do servidor (game bus). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class KN8ServerEvents {

    private KN8ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        KN8Server.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        KN8Server.stop(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        KN8Server instance = KN8Server.getOrNull(event.getServer());
        if (instance != null) {
            instance.endTick();
        }
    }
}
