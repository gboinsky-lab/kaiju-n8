package com.kn8.common.career;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S (0.2, Etapa 5): o menu pede para aceitar ou abandonar uma missao; o servidor valida tudo. */
public record MissionActionC2S(int action, ResourceLocation mission) implements CustomPacketPayload {

    public static final int ACCEPT = 0;
    public static final int ABANDON = 1;

    public static final Type<MissionActionC2S> TYPE = new Type<>(KN8Constants.id("mission_action"));

    public static final StreamCodec<ByteBuf, MissionActionC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MissionActionC2S::action,
            ResourceLocation.STREAM_CODEC, MissionActionC2S::mission,
            MissionActionC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MissionActionC2S payload, ServerPlayer player) {
        if (payload.action() == ACCEPT) {
            MissionService.accept(player, payload.mission());
        } else if (payload.action() == ABANDON) {
            MissionService.abandon(player, payload.mission());
        }
    }
}
