// src/main/java/com/kn8/common/vfx/VfxParticleOptions.java
package com.kn8.common.vfx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Opcoes das particulas proprias do KaijuVFX (VFX-1): cor (0xRRGGBB, tingida pela paleta do perfil), tamanho (em
 * blocos) e vida (ticks). A silhueta vem do tipo ({@code kn8:arc}, {@code kn8:ring}...).
 */
public record VfxParticleOptions(ParticleType<VfxParticleOptions> type, int color, float size, int lifetime)
        implements ParticleOptions {

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    static MapCodec<VfxParticleOptions> codec(ParticleType<VfxParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.fieldOf("color").forGetter(VfxParticleOptions::color),
                Codec.floatRange(0.01F, 16.0F).fieldOf("size").forGetter(VfxParticleOptions::size),
                Codec.intRange(1, 400).fieldOf("lifetime").forGetter(VfxParticleOptions::lifetime)
        ).apply(i, (color, size, lifetime) -> new VfxParticleOptions(type, color, size, lifetime)));
    }

    static StreamCodec<ByteBuf, VfxParticleOptions> streamCodec(ParticleType<VfxParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, VfxParticleOptions::color,
                ByteBufCodecs.FLOAT, VfxParticleOptions::size,
                ByteBufCodecs.VAR_INT, VfxParticleOptions::lifetime,
                (color, size, lifetime) -> new VfxParticleOptions(type, color, size, lifetime));
    }
}
