package com.kn8.client.invasion;

import com.kn8.common.invasion.InvasionStateS2C;
import com.kn8.common.network.KN8ClientHooks;

/**
 * Ultimo estado de invasao recebido do servidor (0.2, Etapa 7), lido pela aba Alertas. So uma copia do que o
 * servidor mandou; limpa ao trocar de mundo pelo proprio servidor (manda NONE no login/troca de dimensao).
 */
public final class ClientInvasion {

    private static volatile InvasionStateS2C state = InvasionStateS2C.NONE;

    private ClientInvasion() {
    }

    public static void install() {
        KN8ClientHooks.register(InvasionStateS2C.TYPE, payload -> state = payload);
    }

    public static InvasionStateS2C state() {
        return state;
    }
}
