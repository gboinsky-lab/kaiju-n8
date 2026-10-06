package com.kn8.common.craft;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C (0.2, Etapa 3): o jogador usou a bancada; o cliente abre a tela de fabricacao (via KN8ClientHooks). */
public record OpenWorkbenchS2C() implements CustomPacketPayload {

    public static final OpenWorkbenchS2C INSTANCE = new OpenWorkbenchS2C();
    public static final Type<OpenWorkbenchS2C> TYPE = new Type<>(KN8Constants.id("open_workbench"));
    public static final StreamCodec<ByteBuf, OpenWorkbenchS2C> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
