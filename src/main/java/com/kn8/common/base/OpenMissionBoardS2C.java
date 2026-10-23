package com.kn8.common.base;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C (PM-8): o jogador falou com um oficial de missoes; o cliente abre o menu na aba Missoes filtrada pelas missoes
 * do {@code giver} dele (o mesmo quadro do menu, sem tela nova).
 */
public record OpenMissionBoardS2C(String giver) implements CustomPacketPayload {

    public static final Type<OpenMissionBoardS2C> TYPE = new Type<>(KN8Constants.id("open_mission_board"));
    public static final StreamCodec<ByteBuf, OpenMissionBoardS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(128), OpenMissionBoardS2C::giver, OpenMissionBoardS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
