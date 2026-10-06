package com.kn8.common.career;

import java.util.List;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * O que o DONO enxerga da propria carreira (menu e HUD). Calculado no servidor, enviado so a ele pelo canal
 * {@code kn8:career}; no cliente fica no attachment {@code kn8:career_view}.
 *
 * @param rank           patente atual
 * @param nextRank       proxima patente (igual a {@code rank} no topo)
 * @param nextMerit      merito exigido pela proxima patente (0 = sem exigencia de merito)
 * @param nextMission    missao de promocao da proxima patente ainda nao concluida (vazio = nenhuma)
 */
public record CareerView(ResourceLocation rank, ResourceLocation nextRank, int merit, int nextMerit,
        String nextMission, Stats stats, List<Active> active, List<ResourceLocation> completed) {

    /** Contadores da aba Perfil. */
    public record Stats(int kaijuKills, int dismantled, int missionsDone) {
        public static final StreamCodec<ByteBuf, Stats> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Stats::kaijuKills, ByteBufCodecs.VAR_INT, Stats::dismantled,
                ByteBufCodecs.VAR_INT, Stats::missionsDone, Stats::new);
    }

    /**
     * Missao aceita: progresso por objetivo, prazo (tempo de jogo absoluto, -1 = sem prazo: o cliente conta o tempo
     * sozinho, sem reenvio a cada segundo), objetivo atual e o ponto do mundo dele (marcador na HUD; {@link #NO_POINT}
     * = sem ponto).
     */
    public record Active(ResourceLocation id, List<Integer> progress, long deadline, int objective,
            BlockPos point) {
        public static final StreamCodec<ByteBuf, Active> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Active::id,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Active::progress,
                ByteBufCodecs.VAR_LONG, Active::deadline, ByteBufCodecs.VAR_INT, Active::objective,
                BlockPos.STREAM_CODEC, Active::point, Active::new);

        /** Ticks restantes no tempo de jogo dado; -1 = sem prazo. */
        public int remainingTicks(long gameTime) {
            return deadline < 0 ? -1 : (int) Math.max(0, deadline - gameTime);
        }

        public boolean hasPoint() {
            return !point.equals(NO_POINT);
        }
    }

    public static final BlockPos NO_POINT = new BlockPos(0, Integer.MIN_VALUE / 2, 0);

    private record Head(ResourceLocation rank, ResourceLocation nextRank, int merit, int nextMerit,
            String nextMission) {
        static final StreamCodec<ByteBuf, Head> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Head::rank, ResourceLocation.STREAM_CODEC, Head::nextRank,
                ByteBufCodecs.VAR_INT, Head::merit, ByteBufCodecs.VAR_INT, Head::nextMerit,
                ByteBufCodecs.STRING_UTF8, Head::nextMission, Head::new);
    }

    public static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("empty");
    public static final CareerView EMPTY = new CareerView(NONE, NONE, 0, 0, "", new Stats(0, 0, 0), List.of(),
            List.of());

    public static final StreamCodec<ByteBuf, CareerView> STREAM_CODEC = StreamCodec.composite(
            Head.CODEC, view -> new Head(view.rank(), view.nextRank(), view.merit(), view.nextMerit(),
                    view.nextMission()),
            Stats.CODEC, CareerView::stats,
            Active.CODEC.apply(ByteBufCodecs.list()), CareerView::active,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), CareerView::completed,
            (head, stats, active, completed) -> new CareerView(head.rank(), head.nextRank(), head.merit(),
                    head.nextMerit(), head.nextMission(), stats, active, completed));

    public boolean isTopRank() {
        return rank.equals(nextRank);
    }
}
