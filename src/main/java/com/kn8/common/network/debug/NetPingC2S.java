// src/main/java/com/kn8/common/network/debug/NetPingC2S.java
package com.kn8.common.network.debug;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** C2S de diagnostico do M3: o cliente manda um numero; o servidor devolve um {@link NetPongS2C} com o mesmo. */
public record NetPingC2S(int nonce) implements CustomPacketPayload {

    public static final Type<NetPingC2S> TYPE = new Type<>(KN8Constants.id("net_ping"));

    public static final StreamCodec<ByteBuf, NetPingC2S> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, NetPingC2S::nonce, NetPingC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Handler de servidor (ja passou pelo C2SGuard: rate limit e atraso de debug). */
    public static void handle(NetPingC2S payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new NetPongS2C(payload.nonce(), player.server.getTickCount()));
    }
}
