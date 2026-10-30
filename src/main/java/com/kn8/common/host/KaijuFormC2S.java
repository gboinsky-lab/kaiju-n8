package com.kn8.common.host;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (T-2): o jogador apertou a tecla da forma do Kaiju No. 8. So a intencao (sem dados); o servidor confere se ele e
 * o hospedeiro, a espera e o estado, e transforma ou volta.
 */
public record KaijuFormC2S(boolean partial) implements CustomPacketPayload {

    public static final Type<KaijuFormC2S> TYPE = new Type<>(KN8Constants.id("kaiju_form"));
    /** HOST: {@code partial} = Shift + tecla (so o braco). */
    public static final StreamCodec<ByteBuf, KaijuFormC2S> STREAM_CODEC = ByteBufCodecs.BOOL.map(KaijuFormC2S::new,
            KaijuFormC2S::partial);

    public static void handle(KaijuFormC2S payload, ServerPlayer player) {
        if (payload.partial()) {
            KaijuFormService.togglePartial(player);
        } else {
            KaijuFormService.toggle(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
