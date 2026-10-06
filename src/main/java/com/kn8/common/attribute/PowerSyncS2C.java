// src/main/java/com/kn8/common/attribute/PowerSyncS2C.java
package com.kn8.common.attribute;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Attachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C privado (canal {@code kn8:power} do {@code NetworkSync}): a {@link PowerView} do proprio jogador. Sem id de
 * entidade de proposito (PT1): o cliente sempre grava no jogador local.
 */
public record PowerSyncS2C(PowerView view) implements CustomPacketPayload {

    public static final ResourceLocation CHANNEL = KN8Constants.id("power");
    public static final Type<PowerSyncS2C> TYPE = new Type<>(CHANNEL);

    public static final StreamCodec<ByteBuf, PowerSyncS2C> STREAM_CODEC =
            PowerView.STREAM_CODEC.map(PowerSyncS2C::new, PowerSyncS2C::view);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Roda no cliente; so classes comuns. */
    public static void handleOnClient(PowerSyncS2C payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().setData(KN8Attachments.POWER_VIEW, payload.view()));
    }
}
