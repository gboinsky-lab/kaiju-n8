// src/main/java/com/kn8/common/world/CheckedJigsawStructure.java
package com.kn8.common.world;

import java.util.Optional;

import com.kn8.core.world.SiteTerrain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Local da obra (construcoes C3): o desenho e outra estrutura jigsaw ({@code layout}); antes de gerar, mede o
 * terreno numa grade de passo {@code check_step} no raio {@code check_radius} em volta do ponto e so deixa nascer sem
 * agua na superficie e com desnivel ate {@code max_slope} ({@link SiteTerrain}). Mede na coluna de ruido (antes de
 * arvores e construcoes), entao vale tambem para o /locate. A posicao (vilas e outras construcoes) fica no
 * {@link AvoidingSpreadPlacement} do conjunto.
 */
public final class CheckedJigsawStructure extends Structure {

    public static final MapCodec<CheckedJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("layout").forGetter(s -> s.layout),
            Codec.intRange(8, 256).fieldOf("check_radius").forGetter(s -> s.radius),
            Codec.intRange(4, 64).optionalFieldOf("check_step", 16).forGetter(s -> s.step),
            Codec.intRange(0, 128).optionalFieldOf("max_slope", 12).forGetter(s -> s.maxSlope)
    ).apply(i, CheckedJigsawStructure::new));

    private final Holder<Structure> layout;
    private final int radius;
    private final int step;
    private final int maxSlope;

    private CheckedJigsawStructure(StructureSettings settings, Holder<Structure> layout, int radius, int step,
            int maxSlope) {
        super(settings);
        this.layout = layout;
        this.radius = radius;
        this.step = step;
        this.maxSlope = maxSlope;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // O jigsaw comeca no canto do chunk; o centro da peca inicial fica a ate meia peca dali.
        int x0 = context.chunkPos().getMinBlockX();
        int z0 = context.chunkPos().getMinBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        SiteTerrain terrain = new SiteTerrain(maxSlope);
        for (int[] offset : SiteTerrain.grid(radius, step)) {
            NoiseColumn column = generator.getBaseColumn(x0 + offset[0], z0 + offset[1], height,
                    context.randomState());
            if (!sample(terrain, column, height)) {
                return Optional.empty();
            }
        }
        return terrain.accepted() ? layout.value().findValidGenerationPoint(context) : Optional.empty();
    }

    /** Primeiro bloco nao-ar de cima para baixo: fluido = agua na superficie; senao, altura do chao. */
    private static boolean sample(SiteTerrain terrain, NoiseColumn column, LevelHeightAccessor height) {
        for (int y = height.getMaxBuildHeight() - 1; y >= height.getMinBuildHeight(); y--) {
            BlockState state = column.getBlock(y);
            if (!state.isAir()) {
                return terrain.add(y, !state.getFluidState().isEmpty());
            }
        }
        return terrain.add(height.getMinBuildHeight(), true);
    }

    @Override
    public StructureType<?> type() {
        return KN8Worldgen.CHECKED_JIGSAW.get();
    }
}
