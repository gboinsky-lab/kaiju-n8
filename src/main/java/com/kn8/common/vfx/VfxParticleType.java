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
        // VFX-2: ignora o limite do vanilla (particula comum so nasce a ate 32 blocos da camera): o efeito de um
        // kaiju de 20 m precisa ser visto de longe. O teto vem do VfxBudget e a distancia do [fx] effectDistance.
        super(true);
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
