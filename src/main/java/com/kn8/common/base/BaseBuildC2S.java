package com.kn8.common.base;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S (PM-8D): construir um modulo num lote da base ({@code module} vazio = demolir); o servidor confere tudo. */
public record BaseBuildC2S(BlockPos core, int slot, String module) implements CustomPacketPayload {

    public static final Type<BaseBuildC2S> TYPE = new Type<>(KN8Constants.id("base_build"));
    public static final StreamCodec<ByteBuf, BaseBuildC2S> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BaseBuildC2S::core, ByteBufCodecs.VAR_INT, BaseBuildC2S::slot,
            ByteBufCodecs.stringUtf8(128), BaseBuildC2S::module, BaseBuildC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BaseBuildC2S payload, ServerPlayer player) {
        if (payload.module().isEmpty()) {
            BaseBuilder.demolish(player, payload.core(), payload.slot());
            return;
        }
        ResourceLocation module = ResourceLocation.tryParse(payload.module());
        if (module != null) {
            BaseBuilder.build(player, payload.core(), payload.slot(), module);
        }
    }
}
