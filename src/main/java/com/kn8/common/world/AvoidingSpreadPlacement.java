// src/main/java/com/kn8/common/world/AvoidingSpreadPlacement.java
package com.kn8.common.world;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Espalhamento aleatorio (igual ao {@code random_spread}) que recusa o chunk se algum conjunto de {@code avoid}
 * puder nascer a ate {@code chunks} chunks dele (construcoes C3: os locais da obra nao caem em cima de vilas, postos
 * de saqueadores, templos...). O {@code exclusion_zone} vanilla so aceita um conjunto. A conferencia fica em
 * {@link #applyInteractionsWithOtherStructures}, que o /locate tambem chama.
 */
public final class AvoidingSpreadPlacement extends RandomSpreadStructurePlacement {

    /** Conjunto a evitar e o raio em chunks. */
    public record Avoid(Holder<StructureSet> set, int chunks) {
        public static final Codec<Avoid> CODEC = RecordCodecBuilder.create(i -> i.group(
                StructureSet.CODEC.fieldOf("structure_set").forGetter(Avoid::set),
                Codec.intRange(1, 32).fieldOf("chunks").forGetter(Avoid::chunks)
        ).apply(i, Avoid::new));
    }

    /** Campos proprios (o construtor de codec da base so junta ate 8 campos de uma vez). */
    private record Spread(int spacing, int separation, RandomSpreadType spreadType, List<Avoid> avoid) {
        static final MapCodec<Spread> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(0, 4096).fieldOf("spacing").forGetter(Spread::spacing),
                Codec.intRange(0, 4096).fieldOf("separation").forGetter(Spread::separation),
                RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                        .forGetter(Spread::spreadType),
                Avoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(Spread::avoid)
        ).apply(i, Spread::new));
    }

    public static final MapCodec<AvoidingSpreadPlacement> CODEC = RecordCodecBuilder.<AvoidingSpreadPlacement>mapCodec(
            i -> placementCodec(i).and(Spread.CODEC.forGetter(p -> p.spread)).apply(i, AvoidingSpreadPlacement::new))
            .validate(p -> p.spacing() <= p.separation()
                    ? DataResult.error(() -> "spacing precisa ser maior que separation")
                    : DataResult.success(p));

    private final Spread spread;

    private AvoidingSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
            Optional<ExclusionZone> exclusion, Spread spread) {
        super(locateOffset, method, frequency, salt, exclusion, spread.spacing(), spread.separation(),
                spread.spreadType());
        this.spread = spread;
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
        if (!super.applyInteractionsWithOtherStructures(state, x, z)) {
            return false;
        }
        for (Avoid avoid : spread.avoid()) {
            if (state.hasStructureChunkInRange(avoid.set(), x, z, avoid.chunks())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructurePlacementType<?> type() {
        return KN8Worldgen.AVOIDING_SPREAD.get();
    }
}
