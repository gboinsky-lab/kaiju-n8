// src/main/java/com/kn8/common/network/debug/NetPongS2C.java
package com.kn8.common.network.debug;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C de diagnostico do M3: resposta ao ping. Tratada no cliente pela ponte {@code KN8ClientHooks}. */
public record NetPongS2C(int nonce, int serverTick) implements CustomPacketPayload {

    public static final Type<NetPongS2C> TYPE = new Type<>(KN8Constants.id("net_pong"));

    public static final StreamCodec<ByteBuf, NetPongS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NetPongS2C::nonce,
            ByteBufCodecs.VAR_INT, NetPongS2C::serverTick,
            NetPongS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
