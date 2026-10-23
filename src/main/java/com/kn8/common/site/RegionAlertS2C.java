package com.kn8.common.site;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C (PM-9): estado de alerta da regiao onde o jogador esta ({@code site} vazio = fora de qualquer regiao). Vai so
 * na mudanca: ao entrar/sair do local e quando o estado da regiao muda. {@code until} em tempo de jogo (o cliente
 * conta sozinho); {@code cause} = id do kaiju ou da invasao.
 */
public record RegionAlertS2C(String site, int state, String cause, long until) implements CustomPacketPayload {

    public static final RegionAlertS2C NONE = new RegionAlertS2C("", 0, "", 0L);
    public static final Type<RegionAlertS2C> TYPE = new Type<>(KN8Constants.id("region_alert"));
    public static final StreamCodec<ByteBuf, RegionAlertS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), RegionAlertS2C::site, ByteBufCodecs.VAR_INT, RegionAlertS2C::state,
            ByteBufCodecs.stringUtf8(128), RegionAlertS2C::cause, ByteBufCodecs.VAR_LONG, RegionAlertS2C::until,
            RegionAlertS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
