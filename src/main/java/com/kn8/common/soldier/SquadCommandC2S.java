// src/main/java/com/kn8/common/soldier/SquadCommandC2S.java
package com.kn8.common.soldier;

import com.kn8.KN8Constants;
import com.kn8.core.soldier.SquadRules;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S (IA-5): pedido da aba Esquadrao do menu. {@code action}: recrutar ou dispensar o soldado {@code entityId}, ou
 * dar a ordem {@code order} (indice de {@link SquadRules.Order}) a todo o esquadrao. O servidor valida tudo
 * ({@link SquadService}).
 */
public record SquadCommandC2S(int action, int entityId, int order) implements CustomPacketPayload {

    public static final int RECRUIT = 0;
    public static final int DISMISS = 1;
    public static final int ORDER = 2;

    public static final Type<SquadCommandC2S> TYPE = new Type<>(KN8Constants.id("squad_command"));

    public static final StreamCodec<ByteBuf, SquadCommandC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SquadCommandC2S::action,
            ByteBufCodecs.VAR_INT, SquadCommandC2S::entityId,
            ByteBufCodecs.VAR_INT, SquadCommandC2S::order,
            SquadCommandC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SquadCommandC2S payload, ServerPlayer player) {
        switch (payload.action()) {
            case RECRUIT -> SquadService.recruit(player, payload.entityId());
            case DISMISS -> SquadService.dismiss(player, payload.entityId());
            case ORDER -> SquadService.order(player, SquadRules.Order.byIndex(payload.order()));
            default -> {
                // Acao desconhecida: ignorada (cliente de outra versao).
            }
        }
    }
}
