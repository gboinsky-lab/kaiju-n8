// src/main/java/com/kn8/common/anim/AnimTriggerS2C.java
package com.kn8.common.anim;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C (Fase 4, secao 5.1): tocar ou parar uma animacao PAL num jogador. Vai para o proprio jogador e para quem o
 * rastreia. {@code serverTick} e o tick em que a acao comecou no servidor: o cliente usa para compensar o atraso
 * (criterio de +-2 ticks) e para o diagnostico {@code /kn8client anim}.
 */
public record AnimTriggerS2C(int entityId, ResourceLocation animation, long serverTick, boolean stop)
        implements CustomPacketPayload {

    public static final Type<AnimTriggerS2C> TYPE = new Type<>(KN8Constants.id("anim_trigger"));

    public static final StreamCodec<ByteBuf, AnimTriggerS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AnimTriggerS2C::entityId,
            ResourceLocation.STREAM_CODEC, AnimTriggerS2C::animation,
            ByteBufCodecs.VAR_LONG, AnimTriggerS2C::serverTick,
            ByteBufCodecs.BOOL, AnimTriggerS2C::stop,
            AnimTriggerS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
