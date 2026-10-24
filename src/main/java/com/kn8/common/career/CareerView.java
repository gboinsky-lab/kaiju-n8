package com.kn8.common.career;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String nextMission, Stats stats, List<Active> active, List<ResourceLocation> completed,
        Map<ResourceLocation, String> bestGrades, Research research) {

    /**
     * PM-8C: pesquisas concluidas e a em andamento ({@link #NONE} = nenhuma; {@code finishTick} em tempo de jogo, o
     * cliente conta o tempo sozinho). PM-11: XP de dominio dos equipamentos numerados (o laboratorio e o menu os
     * mostram juntos; aqui por caber no limite de campos do StreamCodec da visao).
     */
    public record Research(List<ResourceLocation> done, ResourceLocation current, long finishTick,
            Map<ResourceLocation, Integer> mastery) {
        public static final Research EMPTY = new Research(List.of(), NONE, 0L, Map.of());
        public static final StreamCodec<ByteBuf, Research> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), Research::done,
                ResourceLocation.STREAM_CODEC, Research::current, ByteBufCodecs.VAR_LONG, Research::finishTick,
                ByteBufCodecs.<ByteBuf, ResourceLocation, Integer, Map<ResourceLocation, Integer>>map(HashMap::new,
                        ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT), Research::mastery,
                Research::new);

        public boolean busy() {
            return !current.equals(NONE);
        }
    }

    /** Contadores da aba Perfil; tiros dados/acertados em kaiju desde a PM-7 (precisao da carreira). */
    public record Stats(int kaijuKills, int dismantled, int missionsDone, int invasionsDefended, int shotsFired,
            int shotsHit) {
        public static final StreamCodec<ByteBuf, Stats> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Stats::kaijuKills, ByteBufCodecs.VAR_INT, Stats::dismantled,
                ByteBufCodecs.VAR_INT, Stats::missionsDone, ByteBufCodecs.VAR_INT, Stats::invasionsDefended,
                ByteBufCodecs.VAR_INT, Stats::shotsFired, ByteBufCodecs.VAR_INT, Stats::shotsHit,
                Stats::new);
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
    public static final CareerView EMPTY = new CareerView(NONE, NONE, 0, 0, "", new Stats(0, 0, 0, 0, 0, 0),
            List.of(), List.of(), Map.of(), Research.EMPTY);

    public static final StreamCodec<ByteBuf, CareerView> STREAM_CODEC = StreamCodec.composite(
            Head.CODEC, view -> new Head(view.rank(), view.nextRank(), view.merit(), view.nextMerit(),
                    view.nextMission()),
            Stats.CODEC, CareerView::stats,
            Active.CODEC.apply(ByteBufCodecs.list()), CareerView::active,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), CareerView::completed,
            ByteBufCodecs.<ByteBuf, ResourceLocation, String, Map<ResourceLocation, String>>map(HashMap::new,
                    ResourceLocation.STREAM_CODEC, ByteBufCodecs.STRING_UTF8), CareerView::bestGrades,
            Research.CODEC, CareerView::research,
            (head, stats, active, completed, grades, research) -> new CareerView(head.rank(), head.nextRank(),
                    head.merit(), head.nextMerit(), head.nextMission(), stats, active, completed, grades, research));

    public boolean isTopRank() {
        return rank.equals(nextRank);
    }
}
