// src/main/java/com/kn8/common/data/def/SiteDef.java
package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Local da obra ({@code data/<ns>/kn8/site/<id>.json}, construcoes C3): a estrutura do mundo ({@code structure}), a
 * divisao dona do lugar ({@code division}: chave {@code kn8.division.<division>}), a guarnicao (personagens da obra,
 * mesmo formato dos defensores da invasao; aparece uma vez por mundo, na primeira visita) e as invasoes do lugar
 * (sorteadas pelo peso quando a invasao natural cai num jogador dentro dele). Nome: {@code kn8.site.<id>}.
 * {@code field_missions} (CIDADES, Miguel 2026-10-30: "invasoes e missoes dentro delas"): missao de campo aceita ou
 * avancada dentro do local poe os alvos dentro dele (nas cidades).
 */
public record SiteDef(ResourceLocation structure, String division, List<InvasionDef.Defender> garrison,
        List<SiteInvasion> invasions, boolean fieldMissions, Optional<GarrisonAt> garrisonAt) {

    /**
     * BASES-1 (Miguel, 2026-10-30: bases dentro das cidades): onde a guarnicao aparece, pela peca do jigsaw
     * ({@code piece}, id do template) e a posicao dentro dela ({@code pos}, coordenadas do template, antes de girar).
     * Sem isso, na saida oeste da peca inicial.
     */
    public record GarrisonAt(ResourceLocation piece, BlockPos pos) {
        public static final Codec<GarrisonAt> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("piece").forGetter(GarrisonAt::piece),
                BlockPos.CODEC.fieldOf("pos").forGetter(GarrisonAt::pos)
        ).apply(i, GarrisonAt::new));
    }

    /** Invasao do lugar e o peso no sorteio. */
    public record SiteInvasion(ResourceLocation invasion, int weight) {
        public static final Codec<SiteInvasion> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("invasion").forGetter(SiteInvasion::invasion),
                Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(SiteInvasion::weight)
        ).apply(i, SiteInvasion::new));
    }

    public static final Codec<SiteDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("structure").forGetter(SiteDef::structure),
            Codec.STRING.fieldOf("division").forGetter(SiteDef::division),
            InvasionDef.Defender.CODEC.listOf().optionalFieldOf("garrison", List.of()).forGetter(SiteDef::garrison),
            SiteInvasion.CODEC.listOf().optionalFieldOf("invasions", List.of()).forGetter(SiteDef::invasions),
            Codec.BOOL.optionalFieldOf("field_missions", false).forGetter(SiteDef::fieldMissions),
            GarrisonAt.CODEC.optionalFieldOf("garrison_at").forGetter(SiteDef::garrisonAt)
    ).apply(i, SiteDef::new));
}
