package com.kn8.common.research;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C (PM-8C): o jogador usou o terminal de pesquisa; o cliente abre o Arsenal na secao Pesquisa. */
public record OpenResearchS2C() implements CustomPacketPayload {

    public static final OpenResearchS2C INSTANCE = new OpenResearchS2C();
    public static final Type<OpenResearchS2C> TYPE = new Type<>(KN8Constants.id("open_research"));
    public static final StreamCodec<ByteBuf, OpenResearchS2C> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
