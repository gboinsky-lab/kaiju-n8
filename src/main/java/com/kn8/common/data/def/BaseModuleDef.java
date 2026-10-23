package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Modulo da base do jogador ({@code data/<ns>/kn8/base_module/<id>.json}, PM-8D): o template do lote (11 x 9 x 11,
 * em {@code structure/}), o material gasto para construir, a patente exigida e os modulos que precisam existir antes
 * na mesma base. O modulo funciona pelos blocos e NPCs do template (bancada, terminal, estacao, oficiais).
 */
public record BaseModuleDef(ResourceLocation template, List<WorkbenchRecipeDef.Ingredient> cost,
        Optional<ResourceLocation> rank, List<ResourceLocation> requires, int order) {

    public static final Codec<BaseModuleDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("template").forGetter(BaseModuleDef::template),
            WorkbenchRecipeDef.Ingredient.CODEC.listOf().fieldOf("cost").forGetter(BaseModuleDef::cost),
            ResourceLocation.CODEC.optionalFieldOf("rank").forGetter(BaseModuleDef::rank),
            ResourceLocation.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(BaseModuleDef::requires),
            Codec.INT.optionalFieldOf("order", 0).forGetter(BaseModuleDef::order)
    ).apply(i, BaseModuleDef::new));
}
