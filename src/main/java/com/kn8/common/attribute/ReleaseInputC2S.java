// src/main/java/com/kn8/common/attribute/ReleaseInputC2S.java
package com.kn8.common.attribute;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (0.5.0): estado da tecla de Release. {@code direction} +1 = segurando para subir, -1 = segurando para descer
 * (Shift + tecla), 0 = soltou. So vai na mudanca; o servidor sobe/desce a % no proprio tick, confere o traje e o
 * limite. Valores fora de -1..1 viram o sinal (nada alem disso e aceito do cliente).
 */
public record ReleaseInputC2S(int direction) implements CustomPacketPayload {

    public static final Type<ReleaseInputC2S> TYPE = new Type<>(KN8Constants.id("release_input"));

    public static final StreamCodec<ByteBuf, ReleaseInputC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReleaseInputC2S::direction,
            ReleaseInputC2S::new);

    public static void handle(ReleaseInputC2S payload, ServerPlayer player) {
        PowerService.releaseInput(player, Integer.signum(payload.direction()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
