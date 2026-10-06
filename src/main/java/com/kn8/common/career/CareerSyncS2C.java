package com.kn8.common.career;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Attachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C privado (canal {@code kn8:career}): a {@link CareerView} do proprio jogador (padrao do PowerSyncS2C). */
public record CareerSyncS2C(CareerView view) implements CustomPacketPayload {

    public static final ResourceLocation CHANNEL = KN8Constants.id("career");
    public static final Type<CareerSyncS2C> TYPE = new Type<>(CHANNEL);

    public static final StreamCodec<ByteBuf, CareerSyncS2C> STREAM_CODEC =
            CareerView.STREAM_CODEC.map(CareerSyncS2C::new, CareerSyncS2C::view);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Roda no cliente; so classes comuns. */
    public static void handleOnClient(CareerSyncS2C payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().setData(KN8Attachments.CAREER_VIEW, payload.view()));
    }
}
