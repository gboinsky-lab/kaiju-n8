// src/main/java/com/kn8/common/registry/KN8Particles.java
package com.kn8.common.registry;

import com.kn8.KN8Constants;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tipos de particula. Vazio no M1. */
public final class KN8Particles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KN8Constants.MOD_ID);

    private KN8Particles() {
    }
}
