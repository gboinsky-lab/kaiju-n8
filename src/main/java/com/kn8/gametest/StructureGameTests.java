package com.kn8.gametest;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;

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
}
