package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Projeto de pesquisa do laboratorio da base ({@code data/<ns>/kn8/research/<id>.json}, PM-8C). O jogador entrega o
 * material de kaiju ({@code cost}, o mesmo formato dos ingredientes da bancada) no terminal de pesquisa; a pesquisa
 * leva {@code duration_ticks} de tempo de jogo e, pronta, libera os ids de {@code unlocks} (receitas da bancada, como
 * as patentes, mas por outro caminho) e da {@code damage_bonus} contra os kaiju de {@code targets} (id da especie ou
 * {@code #<classe>}: {@code #yoju}, {@code #honju}, {@code #daikaiju}, {@code #numbered}).
 */
public record ResearchDef(List<WorkbenchRecipeDef.Ingredient> cost, int durationTicks, Optional<ResourceLocation> rank,
        List<ResourceLocation> requires, List<ResourceLocation> unlocks, List<String> targets, double damageBonus,
        String category, int order) {

    public static final Codec<ResearchDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            WorkbenchRecipeDef.Ingredient.CODEC.listOf().fieldOf("cost").forGetter(ResearchDef::cost),
            Codec.intRange(1, 10_000_000).optionalFieldOf("duration_ticks", 1200).forGetter(ResearchDef::durationTicks),
            ResourceLocation.CODEC.optionalFieldOf("rank").forGetter(ResearchDef::rank),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(ResearchDef::requires),
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocks", List.of()).forGetter(ResearchDef::unlocks),
            Codec.STRING.listOf().optionalFieldOf("targets", List.of()).forGetter(ResearchDef::targets),
            Codec.doubleRange(0.0, 2.0).optionalFieldOf("damage_bonus", 0.0).forGetter(ResearchDef::damageBonus),
            Codec.STRING.optionalFieldOf("category", "analysis").forGetter(ResearchDef::category),
            Codec.INT.optionalFieldOf("order", 0).forGetter(ResearchDef::order)
    ).apply(i, ResearchDef::new));

    /** O bonus vale contra este kaiju (especie listada ou a classe dele com {@code #}). */
    public boolean affects(ResourceLocation species, KaijuClass kaijuClass) {
        String classTag = "#" + kaijuClass.getSerializedName();
        for (String target : targets) {
            if (target.equals(classTag) || target.equals(species.toString())) {
                return true;
            }
        }
        return false;
    }
}
