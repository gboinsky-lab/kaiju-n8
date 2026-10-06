// src/main/java/com/kn8/common/combat/CombatStateS2C.java
package com.kn8.common.combat;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C privado (Fase 4, secao 5.1): resposta a um pedido de combate e eventos de defesa, so para o proprio jogador.
 * A HUD mostra o passo do combo, "sem stamina", "PARRY!", "CRITICO!" e "guarda quebrada".
 */
public record CombatStateS2C(int action, int result, int comboStep, int comboLength) implements CustomPacketPayload {

    public static final Type<CombatStateS2C> TYPE = new Type<>(KN8Constants.id("combat_state"));

    public static final StreamCodec<ByteBuf, CombatStateS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CombatStateS2C::action,
            ByteBufCodecs.VAR_INT, CombatStateS2C::result,
            ByteBufCodecs.VAR_INT, CombatStateS2C::comboStep,
            ByteBufCodecs.VAR_INT, CombatStateS2C::comboLength,
            CombatStateS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
