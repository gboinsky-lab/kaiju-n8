package com.kn8.gametest;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests das construcoes (0.2): os templates gerados por {@code tools/world/gen_structures.py} carregam com
 * blocos e as estruturas de worldgen existem no registry (senao o /place e o mundo novo nao as teriam).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureGameTests {

    private static final List<String> STRUCTURES = List.of("defense_outpost", "ruined_building", "kaiju_remains",
            "watchtower");
    /** Pecas da base da Forca de Defesa (etapa C1, tools/world/gen_base.py). */
    private static final List<String> BASE_PIECES = List.of("core", "wing_barracks", "wing_armory",
            "wing_training", "wing_hangar", "wing_medical", "corner_px", "corner_nx");
    /** Base grande com a arena de treino (construcoes C2). */
    private static final List<String> LARGE_PIECES = List.of("core", "wing_barracks_armory",
            "wing_training_medical", "wing_hangar_barracks", "wing_armory_training", "wing_medical_hangar",
            "corner_px", "corner_nx", "arena");

    private StructureGameTests() {
    }

    @GameTest(template = "empty_3x3")
    public static void structuresLoad(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (String name : STRUCTURES) {
            Optional<StructureTemplate> template = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id(name));
            helper.assertTrue(template.isPresent() && template.get().getSize().getX() > 0,
                    "Template " + name + " deveria carregar");
            helper.assertTrue(registry.containsKey(ResourceKey.create(Registries.STRUCTURE, KN8Constants.id(name))),
                    "Estrutura " + name + " deveria estar no registry");
        }
        helper.succeed();
    }

    /**
     * Base da Forca de Defesa: as pecas carregam e cada uma tem os encaixes (o centro 4 saidas, a ala 3, o canto 1);
     * a estrutura de varias pecas esta no registry.
     */
    @GameTest(template = "empty_3x3")
    public static void defenseBasePiecesHaveJigsaws(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        helper.assertTrue(registry.containsKey(ResourceKey.create(Registries.STRUCTURE,
                KN8Constants.id("defense_base"))), "A estrutura kn8:defense_base deveria estar no registry");
        for (String piece : BASE_PIECES) {
            Optional<StructureTemplate> template = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id("defense_base/" + piece));
            helper.assertTrue(template.isPresent(), "Peca " + piece + " deveria carregar");
            int jigsaws = template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)
                    .size();
            int expected = piece.equals("core") ? 4 : piece.startsWith("wing") ? 3 : 1;
            helper.assertTrue(jigsaws == expected, "Peca " + piece + " deveria ter " + expected + " encaixes, tem "
                    + jigsaws);
        }
        helper.succeed();
    }

    /** Base grande: pecas com os encaixes certos, a arena com o painel de treino e a estrutura no registry. */
    @GameTest(template = "empty_3x3")
    public static void largeBaseHasArenaConsole(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        helper.assertTrue(registry.containsKey(ResourceKey.create(Registries.STRUCTURE,
                KN8Constants.id("defense_base_large"))), "A base grande deveria estar no registry");
        for (String piece : LARGE_PIECES) {
            Optional<StructureTemplate> template = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id("defense_base_large/" + piece));
            helper.assertTrue(template.isPresent(), "Peca grande " + piece + " deveria carregar");
            int jigsaws = template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)
                    .size();
            int expected = piece.equals("core") ? 4 : piece.startsWith("wing") ? 3 : 1;
            helper.assertTrue(jigsaws == expected, "Peca grande " + piece + " deveria ter " + expected
                    + " encaixes, tem " + jigsaws);
        }
        StructureTemplate arena = helper.getLevel().getStructureManager()
                .get(KN8Constants.id("defense_base_large/arena")).orElseThrow();
        helper.assertTrue(arena.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(),
                KN8Blocks.TRAINING_CONSOLE.get()).size() == 1, "A arena deveria ter o painel de treino");
        helper.succeed();
    }

    /** Cidade (construcoes C2): pecas carregam, com os encaixes certos, e a estrutura esta no registry. */
    @GameTest(template = "empty_3x3")
    public static void cityPiecesHaveJigsaws(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        helper.assertTrue(registry.containsKey(ResourceKey.create(Registries.STRUCTURE, KN8Constants.id("city"))),
                "A cidade deveria estar no registry");
        List<String> pieces = List.of("plaza", "street", "crossing", "street_end", "lot_konbini", "lot_ramen",
                "lot_house", "lot_apartment", "lot_apartment_tall", "lot_office", "lot_park", "lot_parking",
                "lot_ruined");
        for (String piece : pieces) {
            Optional<StructureTemplate> template = helper.getLevel().getStructureManager()
                    .get(KN8Constants.id("city/" + piece));
            helper.assertTrue(template.isPresent(), "Peca da cidade " + piece + " deveria carregar");
            int jigsaws = template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), Blocks.JIGSAW)
                    .size();
            int expected = switch (piece) {
                case "plaza" -> 12;
                case "street" -> 6;
                case "crossing" -> 4;
                default -> 1;
            };
            helper.assertTrue(jigsaws == expected, "Peca da cidade " + piece + " deveria ter " + expected
                    + " encaixes, tem " + jigsaws);
        }
        helper.succeed();
    }
}
