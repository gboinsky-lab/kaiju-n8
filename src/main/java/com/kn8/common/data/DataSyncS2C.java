// src/main/java/com/kn8/common/data/DataSyncS2C.java
package com.kn8.common.data;

import com.kn8.KN8Constants;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: todas as definicoes validadas de UM tipo de dado, codificadas em NBT pelo mesmo Codec do JSON (estado
 * completo; o cliente troca a foto inteira). Enviado no login e depois de cada {@code /reload}.
 */
public record DataSyncS2C(ResourceLocation registry, Tag data) implements CustomPacketPayload {

    public static final Type<DataSyncS2C> TYPE = new Type<>(KN8Constants.id("data_sync"));

    public static final StreamCodec<ByteBuf, DataSyncS2C> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DataSyncS2C::registry,
            // Sem o teto de 2 MB do TAG: os perfis visuais passaram dele na etapa 6 (o cliente caia ao entrar). O
            // pacote vem so do servidor (mesmo formato na rede).
            ByteBufCodecs.TRUSTED_TAG, DataSyncS2C::data,
            DataSyncS2C::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Roda no cliente; so classes comuns. Registry desconhecido = cliente/servidor com versoes diferentes. */
    public static void handleOnClient(DataSyncS2C payload, IPayloadContext context) {
        context.enqueueWork(() -> KN8Data.byId(payload.registry()).ifPresentOrElse(
                registry -> registry.acceptFromServer(payload.data()),
                () -> KN8Constants.LOGGER.warn("[kn8] Dados de tipo desconhecido recebidos: {}", payload.registry())));
    }
}
