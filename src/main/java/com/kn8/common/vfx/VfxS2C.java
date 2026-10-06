// src/main/java/com/kn8/common/vfx/VfxS2C.java
package com.kn8.common.vfx;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * S2C de efeito visual (Etapa D): o servidor decide QUANDO e ONDE; o cliente so desenha (particulas e tremor). Vai
 * para quem esta perto ({@link VfxService#RANGE}).
 */
public record VfxS2C(ResourceLocation effect, Vec3 position, Vec3 direction, float intensity, float shake)
        implements CustomPacketPayload {

    public static final Type<VfxS2C> TYPE = new Type<>(KN8Constants.id("vfx"));

    private static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    public static final StreamCodec<ByteBuf, VfxS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, VfxS2C::effect,
            VEC3, VfxS2C::position,
            VEC3, VfxS2C::direction,
            ByteBufCodecs.FLOAT, VfxS2C::intensity,
            ByteBufCodecs.FLOAT, VfxS2C::shake,
            VfxS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
