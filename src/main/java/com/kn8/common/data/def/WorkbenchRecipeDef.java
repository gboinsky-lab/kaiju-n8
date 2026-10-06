package com.kn8.common.data.def;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Receita da bancada da Forca de Defesa ({@code data/<ns>/kn8/workbench/<id>.json}, 0.2 Etapa 3). Ingredientes
 * saem do inventario; a receita so funciona se a patente do jogador ja liberou {@code unlock} (padrao: o proprio
 * item resultado) nos {@code unlocks} das patentes. Item que nenhuma patente prende fica livre.
 */
public record WorkbenchRecipeDef(ResourceLocation result, int count, List<Ingredient> ingredients, String category,
        Optional<ResourceLocation> unlock, int order) {

    public record Ingredient(ResourceLocation item, int count) {
        public static final Codec<Ingredient> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Ingredient::item),
                Codec.intRange(1, 576).optionalFieldOf("count", 1).forGetter(Ingredient::count)
        ).apply(i, Ingredient::new));
    }

    public static final Codec<WorkbenchRecipeDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("result").forGetter(WorkbenchRecipeDef::result),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(WorkbenchRecipeDef::count),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(WorkbenchRecipeDef::ingredients),
            Codec.STRING.optionalFieldOf("category", "supply").forGetter(WorkbenchRecipeDef::category),
            ResourceLocation.CODEC.optionalFieldOf("unlock").forGetter(WorkbenchRecipeDef::unlock),
            Codec.INT.optionalFieldOf("order", 0).forGetter(WorkbenchRecipeDef::order)
    ).apply(i, WorkbenchRecipeDef::new));

    /** Id que as patentes precisam liberar. */
    public ResourceLocation unlockId() {
        return unlock.orElse(result);
    }
}
