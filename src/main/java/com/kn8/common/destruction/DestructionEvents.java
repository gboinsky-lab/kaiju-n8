// src/main/java/com/kn8/common/destruction/DestructionEvents.java
package com.kn8.common.destruction;

import com.kn8.KN8Constants;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Processa a fila de destruicao e a restauracao de cada dimensao no fim do tick dela (game bus, servidor). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class DestructionEvents {

    private DestructionEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DestructionService.tick(level);
        }
    }
}
