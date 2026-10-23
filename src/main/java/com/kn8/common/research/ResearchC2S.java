package com.kn8.common.research;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S (PM-8C): pedido de comecar uma pesquisa; o servidor confere terminal, patente, pre-requisitos e material. */
public record ResearchC2S(ResourceLocation research) implements CustomPacketPayload {

    public static final Type<ResearchC2S> TYPE = new Type<>(KN8Constants.id("research"));

    public static final StreamCodec<ByteBuf, ResearchC2S> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(ResearchC2S::new, ResearchC2S::research);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchC2S payload, ServerPlayer player) {
        ResearchService.start(player, payload.research());
    }
}
