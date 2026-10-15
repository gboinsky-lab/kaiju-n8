// src/main/java/com/kn8/common/parkour/ParkourStateS2C.java
package com.kn8.common.parkour;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C privado (0.5.0-E): estado continuo de parkour aprovado (ou encerrado) pelo servidor, so para o dono e so na
 * mudanca. Leva os numeros do config que o cliente precisa para mover o jogador tick a tick (o cliente nao le o
 * config do servidor):
 * <ul>
 *   <li>{@code dirX/dirZ}: direcao do wall-run ou do slide; {@code normalX/normalZ}: normal da parede/borda;</li>
 *   <li>{@code speed}: velocidade horizontal (wall-run, inicio do slide);</li>
 *   <li>{@code vertical}: subida inicial do wall-run, queda maxima do wall-slide ou Y dos pes pendurado;</li>
 *   <li>{@code friction}: atrito por tick do slide; {@code maxTicks}: duracao maxima do estado.</li>
 * </ul>
 */
public record ParkourStateS2C(int state, float dirX, float dirZ, float normalX, float normalZ, float speed,
        double vertical, float friction, int maxTicks) implements CustomPacketPayload {

    public static final Type<ParkourStateS2C> TYPE = new Type<>(KN8Constants.id("parkour_state"));

    public static final StreamCodec<ByteBuf, ParkourStateS2C> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> payload.write(buf), ParkourStateS2C::read);

    public static final ParkourStateS2C NONE = new ParkourStateS2C(ParkourState.NONE.ordinal(), 0, 0, 0, 0, 0, 0, 0,
            0);

    public ParkourState parkourState() {
        return ParkourState.byId(state);
    }

    private void write(ByteBuf buf) {
        buf.writeByte(state);
        buf.writeFloat(dirX);
        buf.writeFloat(dirZ);
        buf.writeFloat(normalX);
        buf.writeFloat(normalZ);
        buf.writeFloat(speed);
        buf.writeDouble(vertical);
        buf.writeFloat(friction);
        buf.writeShort(maxTicks);
    }

    private static ParkourStateS2C read(ByteBuf buf) {
        return new ParkourStateS2C(buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readDouble(), buf.readFloat(), buf.readShort());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
