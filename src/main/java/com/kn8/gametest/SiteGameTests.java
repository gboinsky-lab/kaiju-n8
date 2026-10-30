package com.kn8.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SiteDef;
import com.kn8.common.registry.KN8Blocks;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.world.CheckedJigsawStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests dos locais da obra (construcoes C3): as pecas de Ariake, Tachikawa e Shinagawa carregam com os encaixes,
 * os quatro locais conferem o terreno e evitam vilas ({@code kn8:checked_jigsaw}), e os dados dos locais apontam para
 * invasoes e personagens que existem.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiteGameTests {

    private static final List<String> SITES = List.of("ariake_base", "tachikawa_base", "shinagawa_city",
            "tachikawa_city", "exam_ground");
    /** BASES-1: a Base de Tachikawa avulsa nao nasce mais sozinha (fica dentro da cidade); so pelo /place. */
    private static final List<String> NATURAL = List.of("ariake_base", "shinagawa_city", "tachikawa_city",
            "exam_ground");
    /** Peca e quantos encaixes ela tem. */
    private static final Map<String, Integer> PIECES = Map.ofEntries(Map.entry("ariake_base/core", 4),
            Map.entry("ariake_base/harbor", 1), Map.entry("tachikawa_base/core", 4),
            Map.entry("tachikawa_base/airfield", 1), Map.entry("shinagawa/station", 10),
            Map.entry("shinagawa/lot_tower", 1), Map.entry("shinagawa/lot_arcade", 1),
            Map.entry("shinagawa/lot_ant_ruins", 1), Map.entry("shinagawa/street", 6),
            Map.entry("tachikawa/station", 10), Map.entry("tachikawa/lot_department", 1),
            Map.entry("tachikawa/lot_showa_park", 1), Map.entry("tachikawa/lot_stage_garden", 1),
            Map.entry("tachikawa/street", 6), Map.entry("tachikawa/base_core", 2),
            Map.entry("tachikawa/base_training", 1), Map.entry("shinagawa/district", 1));

    private SiteGameTests() {
    }

    @GameTest(template = "empty_3x3")
    public static void sitePiecesHaveJigsaws(GameTestHelper helper) {
        PIECES.forEach((piece, expected) -> {
            Optional<StructureTemplate> template = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id(piece));
            helper.assertTrue(template.isPresent(), "Peca " + piece + " deveria carregar");
            int jigsaws = template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)
                    .size();
            helper.assertTrue(jigsaws == expected, "Peca " + piece + " deveria ter " + expected + " encaixes, tem "
                    + jigsaws);
        });
        StructureTemplate harbor = helper.getLevel().getStructureManager().get(KN8Constants.id("ariake_base/harbor"))
                .orElseThrow();
        helper.assertTrue(harbor.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.WATER).size()
                > 1000, "O porto de Ariake deveria ter a bacia com agua");
        helper.succeed();
    }

    /**
     * CIDADES: toda cidade tem o distrito da Forca de Defesa na saida norte da estacao (um so, sempre) com os
     * servicos do QG dentro; as cidades poem as missoes de campo dentro delas.
     */
    @GameTest(template = "empty_3x3")
    public static void citiesHaveTheDefenseForceDistrict(GameTestHelper helper) {
        // BASES-1: em Tachikawa o distrito virou a base inteira (QG da obra e o campo de treino urbano).
        Map<String, String> districts = Map.of("tachikawa", "tachikawa/base_core", "shinagawa", "shinagawa/district");
        for (String city : List.of("tachikawa", "shinagawa")) {
            StructureTemplate station = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id(city + "/station")).orElseThrow();
            long toDistrict = station.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW).stream()
                    .filter(info -> info.nbt() != null && info.nbt().getString("pool")
                            .equals("kn8:" + districts.get(city))).count();
            helper.assertTrue(toDistrict == 1, city + ": a estacao deveria ter uma saida para o distrito, tem "
                    + toDistrict);
            StructureTemplate district = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id(districts.get(city))).orElseThrow();
            for (Block block : List.of(KN8Blocks.RESEARCH_TERMINAL.get(), KN8Blocks.SQUAD_STATION.get(),
                    KN8Blocks.WEIGHT_RACK.get(), KN8Blocks.DEFENSE_WORKBENCH.get())) {
                helper.assertTrue(!district.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), block).isEmpty(),
                        city + ": o distrito deveria ter " + block);
            }
            helper.assertTrue(KN8Data.SITE.server().get(KN8Constants.id(city + "_city")).fieldMissions(),
                    city + ": a cidade deveria por as missoes de campo dentro dela");
        }
        helper.succeed();
    }

    /**
     * BASES-1: a Base de Tachikawa dentro da cidade tem o campo de treino urbano com o painel da arena na sala de
     * controle no alto e as comportas de ferro ligadas as alavancas pelo redstone; a guarnicao da base (Mina,
     * Hoshina, Kikoru, Reno) e as invasoes dela passaram para a cidade.
     */
    @GameTest(template = "empty_3x3")
    public static void tachikawaBaseIsInsideTheCity(GameTestHelper helper) {
        StructureTemplate training = helper.getLevel().getStructureManager()
                .get(KN8Constants.id("tachikawa/base_training")).orElseThrow();
        var consoles = training.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(),
                KN8Blocks.TRAINING_CONSOLE.get());
        helper.assertTrue(consoles.size() == 1 && consoles.get(0).pos().getY() > 5,
                "O painel deveria ficar na sala de controle no alto");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.IRON_DOOR)
                .size() == 4, "Duas comportas de ferro (porta de duas metades)");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.LEVER).size() == 2,
                "Duas alavancas na sala de controle");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.REDSTONE_WIRE)
                .size() == 20, "Redstone das duas comportas (10 cada)");
        SiteDef city = KN8Data.SITE.server().get(KN8Constants.id("tachikawa_city"));
        helper.assertTrue(city.garrison().size() == 4 && city.garrisonAt().isPresent()
                && city.garrisonAt().get().piece().equals(KN8Constants.id("tachikawa/base_core")),
                "A guarnicao de Tachikawa deveria aparecer dentro da base da cidade");
        helper.assertTrue(city.invasions().stream().anyMatch(entry -> entry.invasion()
                .equals(KN8Constants.id("tachikawa_raid"))), "O raide a base (No. 10) deveria cair na cidade");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3")
    public static void sitesCheckTerrainAndAvoidVillages(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String site : SITES) {
            helper.assertTrue(structures.get(KN8Constants.id(site)) instanceof CheckedJigsawStructure,
                    "kn8:" + site + " deveria conferir o terreno (kn8:checked_jigsaw)");
            helper.assertTrue(structures.containsKey(KN8Constants.id(site + "_layout")),
                    "kn8:" + site + "_layout deveria existir (desenho para o /place)");
        }
        StructureSet set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(KN8Constants.id("defense_sites"));
        helper.assertTrue(set != null && set.structures().size() == NATURAL.size(),
                "kn8:defense_sites deveria ter os " + NATURAL.size() + " locais que nascem sozinhos");
        helper.assertTrue(set.structures().stream().noneMatch(entry -> entry.structure()
                .is(KN8Constants.id("tachikawa_base"))), "A Base de Tachikawa avulsa nao nasce mais sozinha");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(KN8Constants.id("defense_base_large")) == null, "A base grande generica nao nasce mais sozinha");
        helper.succeed();
    }

    /** No mundo plano e seco do teste o local passa na checagem e o desenho gera as pecas. */
    @GameTest(template = "empty_3x3", timeoutTicks = 200)
    public static void siteAcceptsFlatDryGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure site = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(KN8Constants.id("tachikawa_city"));
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        // Alguns chunks seguidos: um ou outro cai perto de um ponto candidato de vila (planicie) e e recusado.
        ChunkPos base = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(4096, 0, 4096));
        boolean accepted = false;
        for (int i = 0; i < 12 && !accepted && site != null; i++) {
            ChunkPos chunk = new ChunkPos(base.x + i * 23, base.z - i * 17);
            Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                    level.getSeed(), chunk, level, biome -> true);
            accepted = site.findValidGenerationPoint(context).isPresent();
        }
        helper.assertTrue(accepted, "No chao plano e seco a cidade de Tachikawa deveria passar na checagem do terreno");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3")
    public static void siteDataIsValid(GameTestHelper helper) {
        Map<net.minecraft.resources.ResourceLocation, SiteDef> sites = KN8Data.SITE.server();
        helper.assertTrue(sites.size() == SITES.size(), "Deveria haver " + SITES.size() + " locais, ha "
                + sites.size());
        sites.forEach((id, def) -> {
            // EXAME: o Campo do Exame de Admissao nao tem invasao propria (o exame e a prova do candidato).
            helper.assertTrue(!def.invasions().isEmpty() || id.getPath().equals("exam_ground"),
                    "Local " + id + " deveria ter invasoes validas");
            def.invasions().forEach(entry -> helper.assertTrue(KN8Data.INVASION.server()
                    .containsKey(entry.invasion()), "Invasao " + entry.invasion() + " deveria existir"));
            def.garrison().stream().filter(member -> !member.variant().equals("rifle")).forEach(member ->
                    helper.assertTrue(KN8Entities.specialSoldier(member.variant()).isPresent(),
                            "Guarnicao de " + id + ": " + member.variant() + " deveria ser um soldado especial"));
        });
        helper.assertTrue(sites.get(KN8Constants.id("tachikawa_base")).division().equals("third"),
                "Tachikawa e da Terceira Divisao");
        helper.assertTrue(sites.get(KN8Constants.id("ariake_base")).division().equals("first"),
                "Ariake e da Primeira Divisao");
        helper.succeed();
    }
}
