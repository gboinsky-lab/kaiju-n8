// src/main/java/com/kn8/common/data/def/RankDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Patente da Forca de Defesa ({@code data/<ns>/kn8/rank/<id>.json}), GDD secao 15. {@code promotion_mission} e a
 * missao de avaliacao para CHEGAR a esta patente; {@code order} define a hierarquia (0 = mais baixa).
 */
public record RankDef(int order, int meritRequired, int releaseCap, float maxHealth,
        Optional<ResourceLocation> promotionMission, int npcSquadSize, List<ResourceLocation> unlocks) {

    public static final Codec<RankDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100).fieldOf("order").forGetter(RankDef::order),
            Codec.intRange(0, 10_000_000).fieldOf("merit_required").forGetter(RankDef::meritRequired),
            // 0.2: a patente nao limita mais o Release (teto em career.releaseMax); campo opcional e ignorado.
            Codec.intRange(0, 100).optionalFieldOf("release_cap", 100).forGetter(RankDef::releaseCap),
            Codec.floatRange(1.0F, 1024.0F).fieldOf("max_health").forGetter(RankDef::maxHealth),
            ResourceLocation.CODEC.optionalFieldOf("promotion_mission").forGetter(RankDef::promotionMission),
            Codec.intRange(0, 64).optionalFieldOf("npc_squad_size", 0).forGetter(RankDef::npcSquadSize),
            DefCodecs.IDS.optionalFieldOf("unlocks", List.of()).forGetter(RankDef::unlocks)
    ).apply(i, RankDef::new));
}
