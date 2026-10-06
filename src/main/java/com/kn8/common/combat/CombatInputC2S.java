// src/main/java/com/kn8/common/combat/CombatInputC2S.java
package com.kn8.common.combat;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S (Fase 4, secao 5.1): intencao de combate. So diz O QUE o jogador quer fazer; o servidor valida arma, estado,
 * stamina e tempo. {@code pressed} serve ao bloqueio (segurar/soltar); a direcao serve a esquiva (vetor horizontal
 * no espaco do mundo, normalizado no servidor).
 */
public record CombatInputC2S(int action, boolean pressed, float dirX, float dirZ) implements CustomPacketPayload {

    public static final Type<CombatInputC2S> TYPE = new Type<>(KN8Constants.id("combat_input"));

    public static final StreamCodec<ByteBuf, CombatInputC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CombatInputC2S::action,
            ByteBufCodecs.BOOL, CombatInputC2S::pressed,
            ByteBufCodecs.FLOAT, CombatInputC2S::dirX,
            ByteBufCodecs.FLOAT, CombatInputC2S::dirZ,
            CombatInputC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
