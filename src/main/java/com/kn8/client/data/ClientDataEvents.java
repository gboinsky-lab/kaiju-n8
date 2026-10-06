// src/main/java/com/kn8/client/data/ClientDataEvents.java
package com.kn8.client.data;

import com.kn8.KN8Constants;
import com.kn8.common.data.DataRegistry;
import com.kn8.common.data.KN8Data;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Lado cliente dos dados (M4): limpa a foto do cliente ao desconectar (nada vaza para o proximo servidor) e
 * oferece {@code /kn8client data}, que mostra quantas definicoes de cada tipo ESTE cliente recebeu.
 */
@EventBusSubscriber(modid = KN8Constants.MOD_ID, value = Dist.CLIENT)
public final class ClientDataEvents {

    private ClientDataEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        KN8Data.clearClient();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("kn8client").then(Commands.literal("data").executes(ctx -> {
            for (DataRegistry<?> registry : KN8Data.ALL) {
                if (registry.syncToClient()) {
                    ctx.getSource().sendSuccess(() -> Component.translatable("kn8.client.data.count",
                            registry.id().getPath(), registry.client().size()), false);
                }
            }
            return 1;
        })));
    }
}
