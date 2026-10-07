package com.kn8.gametest;

import java.util.List;
import java.util.Optional;

import com.kn8.KN8Constants;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;
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
}
