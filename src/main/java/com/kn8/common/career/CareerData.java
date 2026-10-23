package com.kn8.common.career;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Carreira do jogador na Forca de Defesa (attachment {@code kn8:career}, PRIVADO: vai so ao dono pelo
 * {@code NetworkSync}). 0.2, Etapas 2 e 5: patente, merito, contadores e o estado das missoes.
 *
 * <p>Persistido e copiado na morte. Volatil: a janela de XP do boneco de treino (limite por minuto).</p>
 */
public final class CareerData {

    /**
     * Progresso de uma missao aceita: um contador por objetivo, o tick em que foi aceita, os pontos do mundo da
     * missao (patrulha/area a alcancar, na ordem dos objetivos que usam ponto) e, desde a PM-7, o que foi medido
     * nela para a avaliacao (missao salva antes da PM-7 comeca com tudo zerado).
     */
    public record MissionProgress(List<Integer> progress, long acceptedTick, List<BlockPos> points,
            MissionStats stats, Optional<BlockPos> origin) {
        public static final Codec<MissionProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().fieldOf("progress").forGetter(MissionProgress::progress),
                Codec.LONG.optionalFieldOf("accepted", 0L).forGetter(MissionProgress::acceptedTick),
                BlockPos.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(MissionProgress::points),
                MissionStats.CODEC.optionalFieldOf("stats").forGetter(p -> Optional.of(p.stats())),
                BlockPos.CODEC.optionalFieldOf("origin").forGetter(MissionProgress::origin)
        ).apply(i, (progress, accepted, points, stats, origin) -> new MissionProgress(progress, accepted, points,
                stats.orElseGet(MissionStats::new), origin)));

        public MissionProgress(List<Integer> progress, long acceptedTick, List<BlockPos> points) {
            this(progress, acceptedTick, points, new MissionStats(), Optional.empty());
        }
    }

    /**
     * PM-7: medidas de uma missao em andamento (tiros dados e acertados em kaiju, dano causado em kaiju, dano
     * recebido, aliados do esquadrao perdidos). Mudam no lugar; salvas com o progresso.
     */
    public static final class MissionStats {
        public static final Codec<MissionStats> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("shots", 0).forGetter(s -> s.shots),
                Codec.INT.optionalFieldOf("hits", 0).forGetter(s -> s.hits),
                Codec.FLOAT.optionalFieldOf("dealt", 0.0F).forGetter(s -> s.damageDealt),
                Codec.FLOAT.optionalFieldOf("taken", 0.0F).forGetter(s -> s.damageTaken),
                Codec.INT.optionalFieldOf("allies_lost", 0).forGetter(s -> s.alliesLost)
        ).apply(i, MissionStats::new));

        public int shots;
        public int hits;
        public float damageDealt;
        public float damageTaken;
        public int alliesLost;

        public MissionStats() {
        }

        private MissionStats(int shots, int hits, float damageDealt, float damageTaken, int alliesLost) {
            this.shots = shots;
            this.hits = hits;
            this.damageDealt = damageDealt;
            this.damageTaken = damageTaken;
            this.alliesLost = alliesLost;
        }
    }

    /** PM-8C: pesquisa em andamento no laboratorio e o tick (tempo de jogo) em que fica pronta. */
    public record Researching(ResourceLocation id, long finishTick) {
        public static final Codec<Researching> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Researching::id),
                Codec.LONG.fieldOf("finish").forGetter(Researching::finishTick)
        ).apply(i, Researching::new));
    }

    // Persistidos
    private ResourceLocation rank;
    private int merit;
    private int kaijuKills;
    private int dismantled;
    private int missionsDone;
    private int invasionsDefended;
    private final Map<ResourceLocation, MissionProgress> active = new HashMap<>();
    private final Set<ResourceLocation> completed = new HashSet<>();
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();
    // PM-7: precisao de toda a carreira e melhor nota de cada missao (letra S..D)
    private int shotsFired;
    private int shotsHit;
    private final Map<ResourceLocation, String> bestGrades = new HashMap<>();
    // PM-8C: pesquisas concluidas e a em andamento (uma por vez)
    private final Set<ResourceLocation> research = new HashSet<>();
    private Researching researching;

    // Volateis
    private long dummyWindowStart = Long.MIN_VALUE;
    private int dummyXpInWindow;

    public static final Codec<CareerData> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("rank").forGetter(data -> Optional.ofNullable(data.rank)),
            Codec.INT.optionalFieldOf("merit", 0).forGetter(CareerData::merit),
            Codec.INT.optionalFieldOf("kaiju_kills", 0).forGetter(CareerData::kaijuKills),
            Codec.INT.optionalFieldOf("dismantled", 0).forGetter(CareerData::dismantled),
            Codec.INT.optionalFieldOf("missions_done", 0).forGetter(CareerData::missionsDone),
            Codec.unboundedMap(ResourceLocation.CODEC, MissionProgress.CODEC).optionalFieldOf("active", Map.of())
                    .forGetter(data -> Map.copyOf(data.active)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("completed", List.of())
                    .forGetter(data -> List.copyOf(data.completed)),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of())
                    .forGetter(data -> Map.copyOf(data.cooldownUntil)),
            Codec.INT.optionalFieldOf("invasions_defended", 0).forGetter(CareerData::invasionsDefended),
            Codec.INT.optionalFieldOf("shots_fired", 0).forGetter(CareerData::shotsFired),
            Codec.INT.optionalFieldOf("shots_hit", 0).forGetter(CareerData::shotsHit),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.STRING).optionalFieldOf("best_grades", Map.of())
                    .forGetter(data -> Map.copyOf(data.bestGrades)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("research", List.of())
                    .forGetter(data -> List.copyOf(data.research)),
            Researching.CODEC.optionalFieldOf("researching").forGetter(CareerData::researching)
    ).apply(i, CareerData::new));

    public CareerData() {
    }

    private CareerData(Optional<ResourceLocation> rank, int merit, int kaijuKills, int dismantled, int missionsDone,
            Map<ResourceLocation, MissionProgress> active, List<ResourceLocation> completed,
            Map<ResourceLocation, Long> cooldowns, int invasionsDefended, int shotsFired, int shotsHit,
            Map<ResourceLocation, String> bestGrades, List<ResourceLocation> research,
            Optional<Researching> researching) {
        this.rank = rank.orElse(null);
        this.research.addAll(research);
        this.researching = researching.orElse(null);
        this.merit = merit;
        this.kaijuKills = kaijuKills;
        this.dismantled = dismantled;
        this.missionsDone = missionsDone;
        this.invasionsDefended = invasionsDefended;
        this.shotsFired = shotsFired;
        this.shotsHit = shotsHit;
        this.bestGrades.putAll(bestGrades);
        // O Codec devolve listas imutaveis: o progresso e alterado no lugar.
        active.forEach((id, progress) -> this.active.put(id, new MissionProgress(new ArrayList<>(progress.progress()),
                progress.acceptedTick(), new ArrayList<>(progress.points()), progress.stats(),
                progress.origin())));
        this.completed.addAll(completed);
        this.cooldownUntil.putAll(cooldowns);
    }

    /** Patente gravada; vazio = ainda nao definida (o servico usa a de menor ordem dos dados). */
    public Optional<ResourceLocation> rank() {
        return Optional.ofNullable(rank);
    }

    void setRank(ResourceLocation value) {
        rank = value;
    }

    public int merit() {
        return merit;
    }

    void setMerit(int value) {
        merit = Math.max(0, value);
    }

    public int kaijuKills() {
        return kaijuKills;
    }

    void addKaijuKill() {
        kaijuKills++;
    }

    public int dismantled() {
        return dismantled;
    }

    void addDismantled() {
        dismantled++;
    }

    public int missionsDone() {
        return missionsDone;
    }

    void addMissionDone() {
        missionsDone++;
    }

    public int invasionsDefended() {
        return invasionsDefended;
    }

    /** 0.2 (Etapa 7): invasao vencida com o jogador na area. */
    public void addInvasionDefended() {
        invasionsDefended++;
    }

    public int shotsFired() {
        return shotsFired;
    }

    public int shotsHit() {
        return shotsHit;
    }

    /** PM-7: tiro de arma de fogo do jogador (acertou = o raio pegou um kaiju). */
    void addShot(boolean hit) {
        shotsFired++;
        if (hit) {
            shotsHit++;
        }
    }

    /** Melhor nota de cada missao concluida (letra S..D). */
    public Map<ResourceLocation, String> bestGrades() {
        return bestGrades;
    }

    /** PM-8C: pesquisas concluidas. */
    public Set<ResourceLocation> research() {
        return research;
    }

    public Optional<Researching> researching() {
        return Optional.ofNullable(researching);
    }

    public void setResearching(Researching value) {
        researching = value;
    }

    public Map<ResourceLocation, MissionProgress> active() {
        return active;
    }

    public Set<ResourceLocation> completed() {
        return completed;
    }

    public Map<ResourceLocation, Long> cooldowns() {
        return cooldownUntil;
    }

    /** Copia das missoes ativas, em ordem estavel (para a visao do cliente). */
    List<Map.Entry<ResourceLocation, MissionProgress>> activeSorted() {
        List<Map.Entry<ResourceLocation, MissionProgress>> list = new ArrayList<>(active.entrySet());
        list.sort(Map.Entry.comparingByKey());
        return list;
    }

    long dummyWindowStart() {
        return dummyWindowStart;
    }

    int dummyXpInWindow() {
        return dummyXpInWindow;
    }

    void setDummyWindow(long start, int xp) {
        dummyWindowStart = start;
        dummyXpInWindow = xp;
    }
}
