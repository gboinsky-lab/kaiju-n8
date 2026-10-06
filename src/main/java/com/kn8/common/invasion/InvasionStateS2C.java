package com.kn8.common.invasion;

import com.kn8.KN8Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C publico (0.2, Etapa 7): estado da invasao da dimensao do jogador, para a aba Alertas. Enviado so quando muda
 * (fase, onda, kaiju restantes) e no login/troca de dimensao. {@code phase} = ordinal de {@link Invasion.Phase};
 * {@code -1} = nenhuma invasao. O relogio e calculado no cliente a partir de {@code timerEnd} (tempo do mundo).
 */
public record InvasionStateS2C(int phase, ResourceLocation invasion, BlockPos center, int wave, int waves,
        int remaining, int total, long timerEnd) implements CustomPacketPayload {

    public static final InvasionStateS2C NONE = new InvasionStateS2C(-1, KN8Constants.id("none"), BlockPos.ZERO, 0,
            0, 0, 0, 0L);

    public static final Type<InvasionStateS2C> TYPE = new Type<>(KN8Constants.id("invasion_state"));

    public static final StreamCodec<FriendlyByteBuf, InvasionStateS2C> STREAM_CODEC =
            StreamCodec.ofMember(InvasionStateS2C::write, InvasionStateS2C::read);

    private static InvasionStateS2C read(FriendlyByteBuf buf) {
        return new InvasionStateS2C(buf.readVarInt(), buf.readResourceLocation(), buf.readBlockPos(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(phase);
        buf.writeResourceLocation(invasion);
        buf.writeBlockPos(center);
        buf.writeVarInt(wave);
        buf.writeVarInt(waves);
        buf.writeVarInt(remaining);
        buf.writeVarInt(total);
        buf.writeVarLong(timerEnd);
    }

    public boolean active() {
        return phase >= 0;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
