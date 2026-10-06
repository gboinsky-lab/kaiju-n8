// src/main/java/com/kn8/common/data/DataEvents.java
package com.kn8.common.data;

import com.kn8.KN8Constants;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ciclo de vida dos dados (game bus):
 * <ol>
 *   <li>{@link AddReloadListenerEvent}: um reload listener por tipo de dado;</li>
 *   <li>{@link TagsUpdatedEvent} (SERVER_DATA_LOAD): dispara depois que TODOS os recursos recarregaram (abertura do
 *   mundo e {@code /reload}); roda a validacao cruzada;</li>
 *   <li>{@link OnDatapackSyncEvent}: login e fim do {@code /reload}; envia aos clientes os tipos sincronizados;</li>
 *   <li>{@link ServerStoppedEvent}: limpa as fotos do servidor.</li>
 * </ol>
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class DataEvents {

    private DataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        KN8Data.ALL.forEach(registry -> event.addListener(registry.createReloadListener()));
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            DataValidation.run();
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(DataEvents::sendAll);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        KN8Data.clearServer();
        DataValidation.clear();
    }

    private static void sendAll(ServerPlayer player) {
        for (DataRegistry<?> registry : KN8Data.ALL) {
            if (registry.syncToClient()) {
                PacketDistributor.sendToPlayer(player, new DataSyncS2C(registry.id(), registry.encodeForClient()));
            }
        }
    }
}
