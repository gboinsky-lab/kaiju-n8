// src/main/java/com/kn8/common/arena/TrainingArenaEvents.java
package com.kn8.common.arena;

import com.kn8.KN8Constants;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Liga a arena de treino ao tick das entidades (so kaiju de treino fazem algo). */
@EventBusSubscriber(modid = KN8Constants.MOD_ID)
public final class TrainingArenaEvents {

    private TrainingArenaEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof KaijuEntity kaiju && !kaiju.level().isClientSide()
                && ServerConfig.SPEC.isLoaded()) {
            TrainingArenaService.tick(kaiju);
        }
    }
}
