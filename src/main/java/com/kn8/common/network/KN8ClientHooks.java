// src/main/java/com/kn8/common/network/KN8ClientHooks.java
package com.kn8.common.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.kn8.KN8Constants;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Ponte entre handlers S2C (registrados em codigo comum) e codigo que so existe no cliente (padrao do PT6).
 *
 * <p>O cliente instala um handler por tipo de payload no setup ({@link #register}); o handler comum so chama
 * {@link #dispatch}. No servidor dedicado nada e instalado e nenhuma classe de cliente e carregada (regra 4).
 * Guarda funcoes de tratamento do cliente, nao estado de jogo.</p>
 */
public final class KN8ClientHooks {

    private static final Map<CustomPacketPayload.Type<?>, Consumer<? extends CustomPacketPayload>> HANDLERS =
            new ConcurrentHashMap<>();

    private KN8ClientHooks() {
    }

    /** Chamado pelo cliente no setup. */
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        HANDLERS.put(type, handler);
    }

    @SuppressWarnings("unchecked")
    static <T extends CustomPacketPayload> void dispatch(T payload) {
        Consumer<T> handler = (Consumer<T>) HANDLERS.get(payload.type());
        if (handler == null) {
            KN8Constants.LOGGER.debug("[kn8] Sem handler de cliente para {}", payload.type().id());
            return;
        }
        handler.accept(payload);
    }
}
