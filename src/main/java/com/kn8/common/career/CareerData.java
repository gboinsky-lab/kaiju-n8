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
     * Progresso de uma missao aceita: um contador por objetivo, o tick em que foi aceita e os pontos do mundo da
     * missao (patrulha/area a alcancar, na ordem dos objetivos que usam ponto).
     */
    public record MissionProgress(List<Integer> progress, long acceptedTick, List<BlockPos> points) {
        public static final Codec<MissionProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().fieldOf("progress").forGetter(MissionProgress::progress),
                Codec.LONG.optionalFieldOf("accepted", 0L).forGetter(MissionProgress::acceptedTick),
                BlockPos.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(MissionProgress::points)
        ).apply(i, MissionProgress::new));
    }

    // Persistidos
    private ResourceLocation rank;
    private int merit;
    private int kaijuKills;
    private int dismantled;
    private int missionsDone;
    private final Map<ResourceLocation, MissionProgress> active = new HashMap<>();
    private final Set<ResourceLocation> completed = new HashSet<>();
    private final Map<ResourceLocation, Long> cooldownUntil = new HashMap<>();

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
                    .forGetter(data -> Map.copyOf(data.cooldownUntil))
    ).apply(i, CareerData::new));

    public CareerData() {
    }

    private CareerData(Optional<ResourceLocation> rank, int merit, int kaijuKills, int dismantled, int missionsDone,
            Map<ResourceLocation, MissionProgress> active, List<ResourceLocation> completed,
            Map<ResourceLocation, Long> cooldowns) {
        this.rank = rank.orElse(null);
        this.merit = merit;
        this.kaijuKills = kaijuKills;
        this.dismantled = dismantled;
        this.missionsDone = missionsDone;
        // O Codec devolve listas imutaveis: o progresso e alterado no lugar.
        active.forEach((id, progress) -> this.active.put(id, new MissionProgress(new ArrayList<>(progress.progress()),
                progress.acceptedTick(), new ArrayList<>(progress.points()))));
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
