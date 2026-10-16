// src/main/java/com/kn8/common/world/CheckedJigsawStructure.java
package com.kn8.common.world;

import java.util.List;
import java.util.Optional;

import com.kn8.core.world.SiteTerrain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Local da obra (construcoes C3): o desenho e outra estrutura jigsaw ({@code layout}). Antes de gerar, na ordem do
 * mais barato para o mais caro: (1) recusa se algum conjunto de {@code avoid} (vilas, postos de saqueadores,
 * templos...) tiver ponto candidato a ate {@code chunks} chunks; (2) confere o bioma no centro; (3) mede o terreno
 * numa grade de passo {@code check_step} no raio {@code check_radius} ({@link SiteTerrain}): ate {@code max_water} das
 * amostras com agua na superficie e desnivel do chao seco ate {@code max_slope}. Tudo aqui dentro porque o /locate
 * desta versao nao chama as regras de posicao do conjunto ({@code exclusion_zone}); assim ele e a geracao concordam.
 * Mede na coluna de ruido (antes de arvores e construcoes).
 */
public final class CheckedJigsawStructure extends Structure {

    /** Conjunto a evitar e o raio em chunks. */
    public record Avoid(Holder<StructureSet> set, int chunks) {
        public static final Codec<Avoid> CODEC = RecordCodecBuilder.create(i -> i.group(
                StructureSet.CODEC.fieldOf("structure_set").forGetter(Avoid::set),
                Codec.intRange(1, 32).fieldOf("chunks").forGetter(Avoid::chunks)
        ).apply(i, Avoid::new));
    }

    public static final MapCodec<CheckedJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("layout").forGetter(s -> s.layout),
            Codec.intRange(8, 256).fieldOf("check_radius").forGetter(s -> s.radius),
            Codec.intRange(4, 64).optionalFieldOf("check_step", 16).forGetter(s -> s.step),
            Codec.intRange(0, 128).optionalFieldOf("max_slope", 12).forGetter(s -> s.maxSlope),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("max_water", 0.0).forGetter(s -> s.maxWater),
            Avoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(s -> s.avoid)
    ).apply(i, CheckedJigsawStructure::new));

    private final Holder<Structure> layout;
    private final int radius;
    private final int step;
    private final int maxSlope;
    private final double maxWater;
    private final List<Avoid> avoid;
    private final int[][] grid;

    private CheckedJigsawStructure(StructureSettings settings, Holder<Structure> layout, int radius, int step,
            int maxSlope, double maxWater, List<Avoid> avoid) {
        super(settings);
        this.layout = layout;
        this.radius = radius;
        this.step = step;
        this.maxSlope = maxSlope;
        this.maxWater = maxWater;
        this.avoid = avoid;
        this.grid = SiteTerrain.grid(radius, step);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (nearAvoided(context) || !centerBiomeValid(context)) {
            return Optional.empty();
        }
        // O jigsaw comeca no canto do chunk; o centro da peca inicial fica a ate meia peca dali.
        int x0 = context.chunkPos().getMinBlockX();
        int z0 = context.chunkPos().getMinBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        SiteTerrain terrain = new SiteTerrain(maxSlope, SiteTerrain.allowedWet(grid.length, maxWater));
        for (int[] offset : grid) {
            NoiseColumn column = generator.getBaseColumn(x0 + offset[0], z0 + offset[1], height,
                    context.randomState());
            if (!sample(terrain, column, height)) {
                return Optional.empty();
            }
        }
        return terrain.accepted() ? layout.value().findValidGenerationPoint(context) : Optional.empty();
    }

    /**
     * Algum conjunto evitado pode nascer perto? Para o espalhamento aleatorio, cada regiao de {@code spacing} chunks
     * tem um ponto candidato (calculado pela semente, sem gerar nada); confere o das regioes que cobrem o raio.
     */
    private boolean nearAvoided(GenerationContext context) {
        ChunkPos here = context.chunkPos();
        long seed = context.seed();
        for (Avoid entry : avoid) {
            StructurePlacement placement = entry.set().value().placement();
            if (!(placement instanceof RandomSpreadStructurePlacement spread) || spread.spacing() <= 0) {
                continue;
            }
            int spacing = spread.spacing();
            int range = entry.chunks();
            for (int rx = Math.floorDiv(here.x - range, spacing); rx <= Math.floorDiv(here.x + range, spacing); rx++) {
                for (int rz = Math.floorDiv(here.z - range, spacing); rz <= Math.floorDiv(here.z + range, spacing);
                        rz++) {
                    ChunkPos candidate = spread.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                    if (SiteTerrain.within(here.x, here.z, candidate.x, candidate.z, range)
                            && spread.applyAdditionalChunkRestrictions(candidate.x, candidate.z, seed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Bioma no centro antes do terreno (barato: evita medir o mar inteiro); o desenho confere de novo no ponto. */
    private static boolean centerBiomeValid(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        int y = context.chunkGenerator().getSeaLevel();
        return context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler()));
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
