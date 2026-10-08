// src/main/java/com/kn8/common/combat/AmmoS2C.java
package com.kn8.common.combat;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C privado (0.5.0-D): pente da arma de fogo na mao, so para o dono e so na mudanca (tiro, troca de arma, etapa da
 * recarga). {@code magazine} 0 = arma sem pente (a HUD esconde); {@code reloadLeft}/{@code reloadTotal} em ticks.
 */
public record AmmoS2C(int rounds, int magazine, int reloadLeft, int reloadTotal) implements CustomPacketPayload {

    public static final Type<AmmoS2C> TYPE = new Type<>(KN8Constants.id("ammo"));

    public static final StreamCodec<ByteBuf, AmmoS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AmmoS2C::rounds,
            ByteBufCodecs.VAR_INT, AmmoS2C::magazine,
            ByteBufCodecs.VAR_INT, AmmoS2C::reloadLeft,
            ByteBufCodecs.VAR_INT, AmmoS2C::reloadTotal,
            AmmoS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
