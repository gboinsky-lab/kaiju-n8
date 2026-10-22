// src/main/java/com/kn8/common/vfx/VfxS2C.java
package com.kn8.common.vfx;

import java.util.Optional;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * S2C de efeito visual (Etapa D): o servidor decide QUANDO e ONDE; o cliente so desenha (particulas e tremor). Vai
 * para quem esta perto ({@link VfxService#RANGE}). VFX-1 (protocolo 20): leva tambem de QUEM e o efeito (perfil
 * visual {@code vfx_profile/<id>}) e a chave da receita (habilidade/tecnica); o cliente monta o resto com os dados
 * que ja tem. Um pacote por efeito, nunca por particula (diretriz KaijuVFX §4).
 */
public record VfxS2C(ResourceLocation effect, Vec3 position, Vec3 direction, float intensity, float shake,
        Source source) implements CustomPacketPayload {

    /** Perfil visual de quem fez o efeito e chave da receita (vazia = id do efeito). */
    public record Source(Optional<ResourceLocation> profile, String key) {
        public static final Source NONE = new Source(Optional.empty(), "");

        static final StreamCodec<ByteBuf, Source> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Source::profile,
                ByteBufCodecs.stringUtf8(64), Source::key,
                Source::new);
    }

    public static final Type<VfxS2C> TYPE = new Type<>(KN8Constants.id("vfx"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<ByteBuf, VfxS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, VfxS2C::effect,
            VEC3, VfxS2C::position,
            VEC3, VfxS2C::direction,
            ByteBufCodecs.FLOAT, VfxS2C::intensity,
            ByteBufCodecs.FLOAT, VfxS2C::shake,
            Source.STREAM_CODEC, VfxS2C::source,
            VfxS2C::new);

    public VfxS2C(ResourceLocation effect, Vec3 position, Vec3 direction, float intensity, float shake) {
        this(effect, position, direction, intensity, shake, Source.NONE);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
