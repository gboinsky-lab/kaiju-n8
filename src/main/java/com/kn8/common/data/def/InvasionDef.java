package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Alerta de invasao ({@code data/<ns>/kn8/invasion/<id>.json}, 0.2 Etapa 7): sirene durante {@code warning_ticks},
 * depois ondas de kaiju chegando de fora para dentro da area. A proxima onda vem {@code delay_ticks} depois de a
 * anterior ser eliminada. Soldados de defesa surgem no centro; quem esteve na area no fim ganha a recompensa.
 * {@code natural_weight} 0 = so por comando (ou missao). {@code level} (0.3): nivel de 1 a 5 mostrado ao jogador.
 */
public record InvasionDef(int warningTicks, List<Wave> waves, float spawnMin, float spawnMax, float radius,
        int timeLimitTicks, List<Defender> defenders, MissionDef.Rewards rewards, int naturalWeight, int level) {

    public record Spawn(ResourceLocation species, int count) {
        public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("species").forGetter(Spawn::species),
                Codec.intRange(1, 32).optionalFieldOf("count", 1).forGetter(Spawn::count)
        ).apply(i, Spawn::new));
    }

    /**
     * Uma onda: kaiju comuns e, opcionalmente, um chefe ({@code boss/*.json}). {@code mass_revive} (0.3): os
     * numerados desta onda (Kaiju No. 9) revivem de uma vez TODAS as carcacas da area, sem o limite normal.
     */
    public record Wave(List<Spawn> kaiju, Optional<ResourceLocation> boss, int delayTicks, boolean massRevive) {
        public static final Codec<Wave> CODEC = RecordCodecBuilder.create(i -> i.group(
                Spawn.CODEC.listOf().optionalFieldOf("kaiju", List.of()).forGetter(Wave::kaiju),
                ResourceLocation.CODEC.optionalFieldOf("boss").forGetter(Wave::boss),
                DefCodecs.TICKS.optionalFieldOf("delay_ticks", 400).forGetter(Wave::delayTicks),
                Codec.BOOL.optionalFieldOf("mass_revive", false).forGetter(Wave::massRevive)
        ).apply(i, Wave::new));

        public int size() {
            return kaiju.stream().mapToInt(Spawn::count).sum() + (boss.isPresent() ? 1 : 0);
        }
    }

    /** Soldados de defesa (variante e nivel do {@code soldier/soldier_1.json}). */
    public record Defender(String variant, String level, int count) {
        public static final Codec<Defender> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("variant").forGetter(Defender::variant),
                Codec.STRING.optionalFieldOf("level", "normal").forGetter(Defender::level),
                Codec.intRange(1, 16).optionalFieldOf("count", 1).forGetter(Defender::count)
        ).apply(i, Defender::new));
    }

    public static final Codec<InvasionDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            DefCodecs.TICKS.optionalFieldOf("warning_ticks", 400).forGetter(InvasionDef::warningTicks),
            Wave.CODEC.listOf().fieldOf("waves").forGetter(InvasionDef::waves),
            Codec.floatRange(8.0F, 256.0F).optionalFieldOf("spawn_min", 36.0F).forGetter(InvasionDef::spawnMin),
            Codec.floatRange(8.0F, 256.0F).optionalFieldOf("spawn_max", 56.0F).forGetter(InvasionDef::spawnMax),
            Codec.floatRange(16.0F, 512.0F).optionalFieldOf("radius", 96.0F).forGetter(InvasionDef::radius),
            DefCodecs.TICKS.optionalFieldOf("time_limit_ticks", 18_000).forGetter(InvasionDef::timeLimitTicks),
            Defender.CODEC.listOf().optionalFieldOf("defenders", List.of()).forGetter(InvasionDef::defenders),
            MissionDef.Rewards.CODEC.optionalFieldOf("rewards", MissionDef.Rewards.NONE)
                    .forGetter(InvasionDef::rewards),
            Codec.intRange(0, 1000).optionalFieldOf("natural_weight", 1).forGetter(InvasionDef::naturalWeight),
            Codec.intRange(1, 5).optionalFieldOf("level", 1).forGetter(InvasionDef::level)
    ).apply(i, InvasionDef::new));

    public int totalKaiju() {
        return waves.stream().mapToInt(Wave::size).sum();
    }
}
