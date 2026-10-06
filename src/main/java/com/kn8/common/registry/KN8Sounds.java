// src/main/java/com/kn8/common/registry/KN8Sounds.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sons do mod. Vazio no M1. */
public final class KN8Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KN8Constants.MOD_ID);

    private KN8Sounds() {
    }
}
