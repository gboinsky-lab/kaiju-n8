// src/main/java/com/kn8/common/registry/KN8Particles.java
package com.kn8.common.registry;

import java.util.Map;

import com.kn8.KN8Constants;
import com.kn8.common.vfx.VfxParticleType;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tipos de particula. VFX-1 (diretriz KaijuVFX): silhuetas proprias, tingidas pela paleta de cada personagem/especie
 * ({@code vfx_profile/<id>.json}); a textura branca fica em {@code textures/particle/<nome>.png}.
 */
public final class KN8Particles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KN8Constants.MOD_ID);

    /** Pedaco de arco de corte (lamina, garra). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> ARC = register("arc");
    /** Anel plano que cresce no chao (onda de choque, geada). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> RING = register("ring");
    /** Lasca/cristal (quitina, gelo, pedra). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> SHARD = register("shard");
    /** Risco de vento/velocidade (bala, investida, asa). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> STREAK = register("streak");
    /** Ponto de energia que brilha (nucleo, carga, feixe). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> GLOW = register("glow");
    /** VFX-2: anel virado para a camera que cresce rapido (ar comprimido de um soco, onda de rugido). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> PULSE = register("pulse");
    /** Esporo/vapor lento (fungo, revivido, acido). */
    public static final DeferredHolder<ParticleType<?>, VfxParticleType> SPORE = register("spore");

    private KN8Particles() {
    }

    private static DeferredHolder<ParticleType<?>, VfxParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, VfxParticleType::new);
    }

    /** Silhueta pelo nome do JSON ({@code arc}, {@code ring}...). */
    public static Map<String, DeferredHolder<ParticleType<?>, VfxParticleType>> shapes() {
        return Map.of("arc", ARC, "ring", RING, "shard", SHARD, "streak", STREAK, "glow", GLOW, "spore", SPORE,
                "pulse", PULSE);
    }
}
