// src/main/java/com/kn8/common/vfx/VfxParticleType.java
package com.kn8.common.vfx;

import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Tipo de particula do KaijuVFX (VFX-1): uma silhueta por tipo, cor/tamanho/vida pelas opcoes. */
public final class VfxParticleType extends ParticleType<VfxParticleOptions> {

    private final MapCodec<VfxParticleOptions> codec = VfxParticleOptions.codec(this);
    private final StreamCodec<ByteBuf, VfxParticleOptions> streamCodec = VfxParticleOptions.streamCodec(this);

    public VfxParticleType() {
        // Sem furar o limite de particulas do vanilla: o teto do efeito ja vem do VfxBudget.
        super(false);
    }

    @Override
    public MapCodec<VfxParticleOptions> codec() {
        return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, VfxParticleOptions> streamCodec() {
        return streamCodec;
    }

    public VfxParticleOptions with(int color, float size, int lifetime) {
        return new VfxParticleOptions(this, color, size, lifetime);
    }
}
