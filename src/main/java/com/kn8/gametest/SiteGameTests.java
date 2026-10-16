package com.kn8.gametest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.SiteDef;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.world.CheckedJigsawStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
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
 * os quatro locais conferem o terreno e evitam vilas ({@code kn8:checked_jigsaw}), e os dados dos locais apontam para invasoes e personagens que existem.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SiteGameTests {

    private static final List<String> SITES = List.of("ariake_base", "tachikawa_base", "shinagawa_city",
            "tachikawa_city");
    /** Peca e quantos encaixes ela tem. */
    private static final Map<String, Integer> PIECES = Map.ofEntries(Map.entry("ariake_base/core", 4),
            Map.entry("ariake_base/harbor", 1), Map.entry("tachikawa_base/core", 4),
            Map.entry("tachikawa_base/airfield", 1), Map.entry("shinagawa/station", 12),
            Map.entry("shinagawa/lot_tower", 1), Map.entry("shinagawa/lot_arcade", 1),
            Map.entry("shinagawa/lot_ant_ruins", 1), Map.entry("shinagawa/street", 6),
            Map.entry("tachikawa/station", 12), Map.entry("tachikawa/lot_department", 1),
            Map.entry("tachikawa/lot_showa_park", 1), Map.entry("tachikawa/lot_stage_garden", 1),
            Map.entry("tachikawa/street", 6));

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
        helper.assertTrue(set != null && set.structures().size() == SITES.size(),
                "kn8:defense_sites deveria ter os 4 locais");
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
        ChunkPos chunk = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(4096, 0, 4096));
        Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                level.getSeed(), chunk, level, biome -> true);
        helper.assertTrue(site != null && site.findValidGenerationPoint(context).isPresent(),
                "No chao plano e seco a cidade de Tachikawa deveria passar na checagem do terreno");
        helper.succeed();
    }

    @GameTest(template = "empty_3x3")
    public static void siteDataIsValid(GameTestHelper helper) {
        Map<net.minecraft.resources.ResourceLocation, SiteDef> sites = KN8Data.SITE.server();
        helper.assertTrue(sites.size() == SITES.size(), "Deveria haver 4 locais, ha " + sites.size());
        sites.forEach((id, def) -> {
            helper.assertTrue(!def.invasions().isEmpty(), "Local " + id + " deveria ter invasoes validas");
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
