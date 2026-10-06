// src/main/java/com/kn8/common/data/def/BossDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Chefe ({@code data/<ns>/kn8/boss/<id>.json}), GDD secao 23. Reaproveita uma especie de kaiju e acrescenta fases,
 * ataques ponderados, arena e recompensa. {@code player_scaling} vazio = usar o valor do config.
 */
public record BossDef(ResourceLocation kaiju, Optional<Double> playerScaling, Optional<ResourceLocation> music,
        Arena arena, List<Phase> phases, Rewards rewards, Optional<Summon> summon, float healthMultiplier) {

    /**
     * 0.2 (decisao do Miguel): o chefe invoca so Yoju da PROPRIA especie. {@code count} por troca de fase (e ao
     * surgir), no maximo {@code max_alive} vivos ao mesmo tempo perto dele.
     */
    public record Summon(ResourceLocation species, int count, int maxAlive) {
        public static final Codec<Summon> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("species").forGetter(Summon::species),
                Codec.intRange(0, 16).optionalFieldOf("count", 2).forGetter(Summon::count),
                Codec.intRange(0, 32).optionalFieldOf("max_alive", 4).forGetter(Summon::maxAlive)
        ).apply(i, Summon::new));
    }

    public record Arena(float radius, int resetAfterTicks) {
        public static final Codec<Arena> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(4.0F, 256.0F).fieldOf("radius").forGetter(Arena::radius),
                DefCodecs.TICKS.optionalFieldOf("reset_after_ticks", 600).forGetter(Arena::resetAfterTicks)
        ).apply(i, Arena::new));
    }

    public record Attack(ResourceLocation ability, int weight) {
        public static final Codec<Attack> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("ability").forGetter(Attack::ability),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(Attack::weight)
        ).apply(i, Attack::new));
    }

    /** Fase ativa enquanto a vida estiver acima de {@code until_health} (fracao de 0 a 1). */
    public record Phase(float untilHealth, int invulnTicks, List<Attack> attacks) {
        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("until_health").forGetter(Phase::untilHealth),
                DefCodecs.TICKS.optionalFieldOf("invuln_ticks", 20).forGetter(Phase::invulnTicks),
                Attack.CODEC.listOf().fieldOf("attacks").forGetter(Phase::attacks)
        ).apply(i, Phase::new));
    }

    public record Rewards(Optional<ResourceLocation> lootTable, int merit) {
        public static final Rewards NONE = new Rewards(Optional.empty(), 0);
        public static final Codec<Rewards> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("loot_table").forGetter(Rewards::lootTable),
                Codec.intRange(0, 1_000_000).optionalFieldOf("merit", 0).forGetter(Rewards::merit)
        ).apply(i, Rewards::new));
    }

    public static final Codec<BossDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("kaiju").forGetter(BossDef::kaiju),
            Codec.doubleRange(0.0, 2.0).optionalFieldOf("player_scaling").forGetter(BossDef::playerScaling),
            ResourceLocation.CODEC.optionalFieldOf("music").forGetter(BossDef::music),
            Arena.CODEC.fieldOf("arena").forGetter(BossDef::arena),
            Phase.CODEC.listOf().fieldOf("phases").forGetter(BossDef::phases),
            Rewards.CODEC.optionalFieldOf("rewards", Rewards.NONE).forGetter(BossDef::rewards),
            Summon.CODEC.optionalFieldOf("summon").forGetter(BossDef::summon),
            Codec.floatRange(0.1F, 20.0F).optionalFieldOf("health_multiplier", 1.0F)
                    .forGetter(BossDef::healthMultiplier)
    ).apply(i, BossDef::new));
}
