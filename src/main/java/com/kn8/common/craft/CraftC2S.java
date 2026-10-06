package com.kn8.common.craft;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** C2S (0.2, Etapa 3): pedido de fabricar uma receita da bancada; o servidor valida tudo. */
public record CraftC2S(ResourceLocation recipe) implements CustomPacketPayload {

    public static final Type<CraftC2S> TYPE = new Type<>(KN8Constants.id("craft"));

    public static final StreamCodec<ByteBuf, CraftC2S> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(CraftC2S::new, CraftC2S::recipe);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CraftC2S payload, ServerPlayer player) {
        WorkbenchService.craft(player, payload.recipe());
    }
}
