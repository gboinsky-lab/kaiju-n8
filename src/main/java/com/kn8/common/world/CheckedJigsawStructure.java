// src/main/java/com/kn8/common/world/CheckedJigsawStructure.java
package com.kn8.common.world;

import java.util.ArrayList;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * Local da obra (construcoes C3): o desenho e outra estrutura jigsaw ({@code layout}). Antes de gerar, na ordem do
 * mais barato para o mais caro: (1) recusa se algum conjunto de {@code avoid} (vilas, postos de saqueadores,
 * templos...) tiver ponto candidato a ate {@code chunks} chunks num bioma em que ele pode nascer; (2) confere o bioma
 * no centro; (3) mede o terreno numa grade de passo {@code check_step} no raio {@code check_radius}
 * ({@link SiteTerrain}): ate {@code max_water} das amostras com agua na superficie e, no miolo
 * ({@code slope_radius}), desnivel do chao seco ate {@code max_slope} (a borda irregular fica com o beard_thin).
 * Tudo aqui dentro porque o /locate desta versao nao chama as regras de posicao do conjunto ({@code exclusion_zone});
 * assim ele e a geracao concordam.
 * Mede na coluna de ruido (antes de arvores e construcoes).
 * BASES-4: com {@code anchor}, so nasce a {@code offsets} chunks de onde a estrutura ancora (a base grande) nasce de
 * verdade: os bairros da cidade ficam em volta da base. O conjunto da ancora e de espalhamento aleatorio; o do bairro
 * e de espacamento 1 (todo chunk e candidato e esta regra escolhe).
 */
public final class CheckedJigsawStructure extends Structure {

    /**
     * Conjunto a evitar e o raio em chunks. Com {@code generated} so conta o ponto candidato em que o conjunto gera
     * alguma estrutura de verdade (as bases grandes: ponto candidato ha em todo lugar, base nao), conferido no fim.
     */
    public record Avoid(Holder<StructureSet> set, int chunks, boolean generated) {
        public static final Codec<Avoid> CODEC = RecordCodecBuilder.create(i -> i.group(
                StructureSet.CODEC.fieldOf("structure_set").forGetter(Avoid::set),
                Codec.intRange(1, 32).fieldOf("chunks").forGetter(Avoid::chunks),
                Codec.BOOL.optionalFieldOf("generated", false).forGetter(Avoid::generated)
        ).apply(i, Avoid::new));
    }

    /** Estrutura em volta da qual esta nasce: o conjunto dela e os deslocamentos possiveis em chunks. */
    public record Anchor(Holder<Structure> structure, Holder<StructureSet> set, List<List<Integer>> offsets) {
        public static final Codec<Anchor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Structure.CODEC.fieldOf("structure").forGetter(Anchor::structure),
                StructureSet.CODEC.fieldOf("structure_set").forGetter(Anchor::set),
                Codec.INT.listOf(2, 2).listOf(1, 16).fieldOf("offsets").forGetter(Anchor::offsets)
        ).apply(i, Anchor::new));
    }

    /** Local que precisa caber junto (a cidade principal da base), a {@code offset} chunks. */
    public record Companion(Holder<Structure> structure, List<Integer> offset) {
        public static final Codec<Companion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Structure.CODEC.fieldOf("structure").forGetter(Companion::structure),
                Codec.INT.listOf(2, 2).fieldOf("offset").forGetter(Companion::offset)
        ).apply(i, Companion::new));
    }

    public static final MapCodec<CheckedJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("layout").forGetter(s -> s.layout),
            Codec.intRange(8, 256).fieldOf("check_radius").forGetter(s -> s.radius),
            Codec.intRange(4, 64).optionalFieldOf("check_step", 16).forGetter(s -> s.step),
            Codec.intRange(0, 128).optionalFieldOf("max_slope", 12).forGetter(s -> s.maxSlope),
            Codec.intRange(0, 256).optionalFieldOf("slope_radius", 64).forGetter(s -> s.slopeRadius),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("max_water", 0.0).forGetter(s -> s.maxWater),
            Avoid.CODEC.listOf().optionalFieldOf("avoid", List.of()).forGetter(s -> s.avoid),
            Anchor.CODEC.optionalFieldOf("anchor").forGetter(s -> s.anchor),
            Companion.CODEC.optionalFieldOf("companion").forGetter(s -> s.companion)
    ).apply(i, CheckedJigsawStructure::new));

    private final Holder<Structure> layout;
    private final int radius;
    private final int step;
    private final int maxSlope;
    private final int slopeRadius;
    private final double maxWater;
    private final List<Avoid> avoid;
    private final Optional<Anchor> anchor;
    private final Optional<Companion> companion;
    private final int[][] grid;

    private CheckedJigsawStructure(StructureSettings settings, Holder<Structure> layout, int radius, int step,
            int maxSlope, int slopeRadius, double maxWater, List<Avoid> avoid, Optional<Anchor> anchor,
            Optional<Companion> companion) {
        super(settings);
        this.layout = layout;
        this.radius = radius;
        this.step = step;
        this.maxSlope = maxSlope;
        this.slopeRadius = slopeRadius;
        this.maxWater = maxWater;
        this.avoid = avoid;
        this.anchor = anchor;
        this.companion = companion;
        this.grid = SiteTerrain.grid(radius, step);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (anchor.isPresent() && !anchoredHere(context, anchor.get())) {
            return Optional.empty();
        }
        Optional<GenerationStub> point = checkedPoint(context);
        if (point.isPresent() && companion.isPresent() && !companionFits(context, companion.get())) {
            return Optional.empty();
        }
        return point;
    }

    /**
     * A base so nasce se a cidade principal tambem couber no lugar dela (as mesmas checagens da cidade, sem a ancora,
     * que voltaria a esta base): assim toda base fica dentro da cidade e so os bairros extras podem faltar.
     */
    private static boolean companionFits(GenerationContext context, Companion companion) {
        if (!(companion.structure().value() instanceof CheckedJigsawStructure city)) {
            return true;
        }
        ChunkPos at = new ChunkPos(context.chunkPos().x + companion.offset().get(0),
                context.chunkPos().z + companion.offset().get(1));
        GenerationContext there = new GenerationContext(context.registryAccess(), context.chunkGenerator(),
                context.biomeSource(), context.randomState(), context.structureTemplateManager(), context.seed(), at,
                context.heightAccessor(), city.biomes()::contains);
        return city.checkedPoint(there).isPresent();
    }

    /** Vilas, bioma, terreno e o desenho (tudo menos a ancora e o local junto). */
    private Optional<GenerationStub> checkedPoint(GenerationContext context) {
        if (nearAvoided(context, false) || !centerBiomeValid(context)) {
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
            int[] surface = surface(column, height);
            boolean core = offset[0] * offset[0] + offset[1] * offset[1] <= slopeRadius * slopeRadius;
            if (!terrain.add(surface[0], surface[1] == 1, core)) {
                return Optional.empty();
            }
        }
        if (!terrain.accepted() || nearAvoided(context, true)) {
            return Optional.empty();
        }
        return layout.value().findValidGenerationPoint(context);
    }

    /**
     * Algum conjunto evitado pode nascer perto? Para o espalhamento aleatorio, cada regiao de {@code spacing} chunks
     * tem um ponto candidato (calculado pela semente, sem gerar nada); confere o das regioes que cobrem o raio.
     * {@code generated} escolhe a passada: as entradas baratas no comeco, as que conferem a geracao no fim.
     */
    private boolean nearAvoided(GenerationContext context, boolean generated) {
        ChunkPos here = context.chunkPos();
        long seed = context.seed();
        for (Avoid entry : avoid) {
            if (entry.generated() != generated) {
                continue;
            }
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
                            && !isOwnAnchor(entry.set(), here, candidate)
                            && spread.applyAdditionalChunkRestrictions(candidate.x, candidate.z, seed)
                            && canSpawnThere(context, entry.set().value(), candidate)
                            && (!generated || chosenAt(context, entry.set().value(), candidate, false) != null)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * A estrutura ancora nasce em algum dos chunks {@code aqui - deslocamento}? Primeiro o ponto candidato do conjunto
     * dela (barato: so a semente); so nele roda a escolha do conjunto como a geracao faz ({@link #chosenAt}). Assim o
     * bairro nunca fica sem a base. Publico para o GameTest.
     */
    public static boolean anchoredHere(GenerationContext context, Anchor anchor) {
        StructureSet set = anchor.set().value();
        if (!(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return false;
        }
        ChunkPos here = context.chunkPos();
        long seed = context.seed();
        for (List<Integer> offset : anchor.offsets()) {
            ChunkPos at = new ChunkPos(here.x - offset.get(0), here.z - offset.get(1));
            if (spread.getPotentialStructureChunk(seed, at.x, at.z).equals(at)
                    && spread.applyAdditionalChunkRestrictions(at.x, at.z, seed)
                    && chosenAt(context, set, at, true) == anchor.structure().value()) {
                return true;
            }
        }
        return false;
    }

    /**
     * So a conta do lugar: algum {@code aqui - deslocamento} e o ponto candidato do conjunto da ancora? (Sem conferir
     * se a base cabe; para o GameTest, que no mundo plano nao tem como deixar so uma base de pe.)
     */
    public static boolean anchorCandidateHere(GenerationContext context, Anchor anchor) {
        if (!(anchor.set().value().placement() instanceof RandomSpreadStructurePlacement spread)) {
            return false;
        }
        ChunkPos here = context.chunkPos();
        for (List<Integer> offset : anchor.offsets()) {
            ChunkPos at = new ChunkPos(here.x - offset.get(0), here.z - offset.get(1));
            if (spread.getPotentialStructureChunk(context.seed(), at.x, at.z).equals(at)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estrutura que o conjunto gera no chunk (ou null): a mesma conta do {@code ChunkGenerator.createStructures} do
     * vanilla (sorteio pelo peso com a semente do chunk; a que nao cabe sai e sorteia de novo), com o contexto e o
     * filtro de bioma que a geracao usaria. Sem {@code withCompanion} nao confere a cidade da base (o bairro so quer
     * saber se ha outra base ali; conferir a cidade dela voltaria a este bairro).
     */
    private static Structure chosenAt(GenerationContext context, StructureSet set, ChunkPos at,
            boolean withCompanion) {
        List<StructureSet.StructureSelectionEntry> left = new ArrayList<>(set.structures());
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(context.seed(), at.x, at.z);
        int total = 0;
        for (StructureSet.StructureSelectionEntry entry : left) {
            total += entry.weight();
        }
        while (!left.isEmpty()) {
            int k = 0;
            if (left.size() > 1 || set.structures().size() > 1) {
                int roll = random.nextInt(total);
                for (StructureSet.StructureSelectionEntry entry : left) {
                    roll -= entry.weight();
                    if (roll < 0) {
                        break;
                    }
                    k++;
                }
            }
            StructureSet.StructureSelectionEntry entry = left.get(k);
            Structure structure = entry.structure().value();
            GenerationContext there = new GenerationContext(context.registryAccess(), context.chunkGenerator(),
                    context.biomeSource(), context.randomState(), context.structureTemplateManager(), context.seed(), at,
                    context.heightAccessor(), structure.biomes()::contains);
            boolean fits = !withCompanion && structure instanceof CheckedJigsawStructure checked
                    ? checked.checkedPoint(there).isPresent() : structure.findValidGenerationPoint(there).isPresent();
            if (fits) {
                return structure;
            }
            left.remove(k);
            total -= entry.weight();
        }
        return null;
    }

    /** O bairro evita as outras bases do conjunto da ancora, nao a propria (que esta a um dos deslocamentos). */
    private boolean isOwnAnchor(Holder<StructureSet> set, ChunkPos here, ChunkPos candidate) {
        if (anchor.isEmpty() || anchor.get().set().value() != set.value()) {
            return false;
        }
        for (List<Integer> offset : anchor.get().offsets()) {
            if (candidate.x == here.x - offset.get(0) && candidate.z == here.z - offset.get(1)) {
                return true;
            }
        }
        return false;
    }

    /** O ponto candidato so conta se o bioma dele aceita alguma estrutura do conjunto (iglu so na neve...). */
    private static boolean canSpawnThere(GenerationContext context, StructureSet set, ChunkPos candidate) {
        Holder<Biome> biome = biomeAt(context, candidate);
        for (StructureSet.StructureSelectionEntry entry : set.structures()) {
            if (entry.structure().value().biomes().contains(biome)) {
                return true;
            }
        }
        return false;
    }

    private static Holder<Biome> biomeAt(GenerationContext context, ChunkPos chunk) {
        return context.biomeSource().getNoiseBiome(QuartPos.fromBlock(chunk.getMiddleBlockX()),
                QuartPos.fromBlock(context.chunkGenerator().getSeaLevel()), QuartPos.fromBlock(chunk.getMiddleBlockZ()),
                context.randomState().sampler());
    }

    /** Bioma no centro antes do terreno (barato: evita medir o mar inteiro); o desenho confere de novo no ponto. */
    private static boolean centerBiomeValid(GenerationContext context) {
        return context.validBiome().test(biomeAt(context, context.chunkPos()));
    }

    /** Primeiro bloco nao-ar de cima para baixo: {altura, 1 se e fluido (agua na superficie)}. */
    private static int[] surface(NoiseColumn column, LevelHeightAccessor height) {
        for (int y = height.getMaxBuildHeight() - 1; y >= height.getMinBuildHeight(); y--) {
            BlockState state = column.getBlock(y);
            if (!state.isAir()) {
                return new int[] {y, state.getFluidState().isEmpty() ? 0 : 1};
            }
        }
        return new int[] {height.getMinBuildHeight(), 1};
    }

    /** Base em volta da qual este bairro nasce (vazio nos outros locais). */
    public Optional<Anchor> anchor() {
        return anchor;
    }

    @Override
    public StructureType<?> type() {
        return KN8Worldgen.CHECKED_JIGSAW.get();
    }
}
