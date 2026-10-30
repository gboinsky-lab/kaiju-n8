package com.kn8.common.attribute;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (AJUSTES, Miguel 2026-10-30: o jogador escolhe a cor da aura): o botao da aba Perfil pede a proxima
 * ({@code step} 1) ou a anterior (-1) das auras de jogador. O servidor so aceita as {@code aura/player_*.json}
 * ({@link PowerService#cyclePlayerAura}).
 */
public record AuraChoiceC2S(int step) implements CustomPacketPayload {

    public static final Type<AuraChoiceC2S> TYPE = new Type<>(KN8Constants.id("aura_choice"));
    public static final StreamCodec<ByteBuf, AuraChoiceC2S> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            AuraChoiceC2S::new, AuraChoiceC2S::step);

    public static void handle(AuraChoiceC2S payload, ServerPlayer player) {
        PowerService.cyclePlayerAura(player, payload.step() < 0 ? -1 : 1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
