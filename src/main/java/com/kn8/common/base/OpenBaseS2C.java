package com.kn8.common.base;

import java.util.List;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C (PM-8D): estado da base do jogador (nucleo e o modulo de cada lote, "" = vazio). {@code open} = abrir o menu
 * na aba Base (usou o nucleo); senao so atualiza a aba aberta (depois de construir ou demolir).
 */
public record OpenBaseS2C(BlockPos core, List<String> slots, boolean open) implements CustomPacketPayload {

    public static final Type<OpenBaseS2C> TYPE = new Type<>(KN8Constants.id("open_base"));
    public static final StreamCodec<ByteBuf, OpenBaseS2C> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenBaseS2C::core,
            ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(PlayerBases.SLOTS)), OpenBaseS2C::slots,
            ByteBufCodecs.BOOL, OpenBaseS2C::open, OpenBaseS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
