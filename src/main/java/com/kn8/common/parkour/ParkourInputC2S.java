// src/main/java/com/kn8/common/parkour/ParkourInputC2S.java
package com.kn8.common.parkour;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (0.5.0-E): intencao de parkour. {@code move} = indice de {@link ParkourMove} (fora da faixa vira STOP);
 * {@code side} = lado da parede que o cliente viu no wall-run (-1/+1, so desempata quando ha parede dos dois lados).
 * O servidor confere tudo de novo.
 */
public record ParkourInputC2S(int move, int side) implements CustomPacketPayload {

    public static final Type<ParkourInputC2S> TYPE = new Type<>(KN8Constants.id("parkour_input"));

    public static final StreamCodec<ByteBuf, ParkourInputC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ParkourInputC2S::move,
            ByteBufCodecs.VAR_INT, ParkourInputC2S::side,
            ParkourInputC2S::new);

    public ParkourInputC2S(ParkourMove move, int side) {
        this(move.ordinal(), side);
    }

    public static void handle(ParkourInputC2S payload, ServerPlayer player) {
        ParkourService.handle(player, ParkourMove.byId(payload.move()), Integer.signum(payload.side()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
