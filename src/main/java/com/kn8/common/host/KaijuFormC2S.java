package com.kn8.common.host;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (T-2): o jogador apertou a tecla da forma do Kaiju No. 8. So a intencao (sem dados); o servidor confere se ele e
 * o hospedeiro, a espera e o estado, e transforma ou volta.
 */
public record KaijuFormC2S() implements CustomPacketPayload {

    public static final Type<KaijuFormC2S> TYPE = new Type<>(KN8Constants.id("kaiju_form"));
    public static final KaijuFormC2S INSTANCE = new KaijuFormC2S();
    public static final StreamCodec<ByteBuf, KaijuFormC2S> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handle(KaijuFormC2S payload, ServerPlayer player) {
        KaijuFormService.toggle(player);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
