// src/main/java/com/kn8/common/registry/KN8Registration.java
package com.kn8.common.registry;

import net.neoforged.bus.api.IEventBus;

/**
 * Liga todos os DeferredRegisters do mod ao mod event bus, em um unico lugar.
 * Os DeferredRegisters sao campos estaticos por exigencia do NeoForge; eles guardam definicoes, nao estado de jogo.
 */
public final class KN8Registration {

    private KN8Registration() {
    }

    public static void register(IEventBus modEventBus) {
        KN8Blocks.BLOCKS.register(modEventBus);
        KN8Items.ITEMS.register(modEventBus);
        KN8Items.TABS.register(modEventBus);
        KN8Entities.ENTITY_TYPES.register(modEventBus);
        KN8Sounds.SOUND_EVENTS.register(modEventBus);
        KN8Particles.PARTICLE_TYPES.register(modEventBus);
        KN8Attachments.ATTACHMENT_TYPES.register(modEventBus);
        KN8DataComponents.DATA_COMPONENTS.register(modEventBus);
    }
}
