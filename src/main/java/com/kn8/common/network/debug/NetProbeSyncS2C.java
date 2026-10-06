// src/main/java/com/kn8/common/network/debug/NetProbeSyncS2C.java
package com.kn8.common.network.debug;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Attachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C do canal privado de teste do M3 ({@code kn8:net_probe}): valida o {@code NetworkSync} como o PT1 validou o
 * padrao. Sem id de entidade de proposito: o cliente sempre grava no proprio jogador local.
 */
public record NetProbeSyncS2C(int value) implements CustomPacketPayload {

    public static final ResourceLocation CHANNEL = KN8Constants.id("net_probe");
    public static final Type<NetProbeSyncS2C> TYPE = new Type<>(CHANNEL);

    public static final StreamCodec<ByteBuf, NetProbeSyncS2C> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, NetProbeSyncS2C::value, NetProbeSyncS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Roda no cliente; so classes comuns (jogador local via contexto). */
    public static void handleOnClient(NetProbeSyncS2C payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().setData(KN8Attachments.NET_PROBE, payload.value()));
    }
}
