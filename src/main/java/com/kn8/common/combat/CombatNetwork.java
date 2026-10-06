// src/main/java/com/kn8/common/combat/CombatNetwork.java
package com.kn8.common.combat;

import net.minecraft.server.level.ServerPlayer;

/** Ponte publica do pacote de combate para o registro de rede (o servico em si fica restrito ao pacote). */
public final class CombatNetwork {

    private CombatNetwork() {
    }

    public static void handle(CombatInputC2S payload, ServerPlayer player) {
        CombatService.handle(payload, player);
    }
}
