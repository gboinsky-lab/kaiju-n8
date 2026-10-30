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
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
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
            "tachikawa_city", "exam_ground", "ariake_city", "tachikawa_ward", "ariake_ward");
    /**
     * BASES-4: no conjunto comum so Shinagawa e o Campo do Exame; cada base tem o conjunto dela e as cidades de
     * Tachikawa e Ariake (com os bairros) nascem em volta da base.
     */
    private static final List<String> NATURAL = List.of("shinagawa_city", "exam_ground");
    /** Peca e quantos encaixes ela tem. */
    private static final Map<String, Integer> PIECES = Map.ofEntries(Map.entry("ariake_base/core", 4),
            Map.entry("ariake_base/harbor", 1), Map.entry("tachikawa_base/core", 4),
            Map.entry("tachikawa_base/airfield", 1), Map.entry("shinagawa/station", 10),
            Map.entry("shinagawa/lot_tower", 1), Map.entry("shinagawa/lot_arcade", 1),
            Map.entry("shinagawa/lot_ant_ruins", 1), Map.entry("shinagawa/street", 6),
            Map.entry("tachikawa/station", 10), Map.entry("tachikawa/lot_department", 1),
            Map.entry("tachikawa/lot_showa_park", 1), Map.entry("tachikawa/lot_stage_garden", 1),
            Map.entry("tachikawa/street", 6), Map.entry("tachikawa/plaza", 12), Map.entry("shinagawa/district", 1),
            Map.entry("tachikawa_base/hq", 4), Map.entry("tachikawa_base/hq_ruined", 4),
            Map.entry("tachikawa_base/training", 1), Map.entry("tachikawa_base/training_ruined", 1),
            Map.entry("tachikawa_base/front", 1), Map.entry("ariake/station", 10), Map.entry("ariake/plaza", 12),
            Map.entry("ariake/street", 6), Map.entry("ariake/lot_bigsight", 1), Map.entry("ariake/lot_warehouse", 1),
            Map.entry("ariake/lot_canal", 1), Map.entry("ariake_base/hq", 4), Map.entry("ariake_base/hq_ruined", 4),
            Map.entry("ariake_base/front", 1));

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
        // BASES-4: Tachikawa e Ariake tem a base grande ao lado (testes abaixo); o distrito ficou em Shinagawa.
        Map<String, String> districts = Map.of("shinagawa", "shinagawa/district");
        for (String city : districts.keySet()) {
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
     * BASES-4: a Base de Tachikawa e uma estrutura grande propria (QG, entrada ao norte, campo de treino urbano a
     * oeste, aerodromo a leste e alas ao sul) com o painel da arena na sala de controle no alto e as comportas de
     * ferro ligadas as alavancas pelo redstone; a guarnicao (Mina, Hoshina, Kikoru, Reno) e o raide ficam na base; a
     * cidade e os bairros nascem em volta dela (anchor) e a estacao nao leva mais a base. B-1: o Kafka na guarnicao.
     */
    @GameTest(template = "empty_3x3")
    public static void tachikawaBaseIsBigAndSurroundedByTheCity(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        var settings = new StructurePlaceSettings();
        StructureTemplate hq = manager.get(KN8Constants.id("tachikawa_base/hq")).orElseThrow();
        for (String pool : List.of("kn8:tachikawa_base/front", "kn8:tachikawa_base/training",
                "kn8:tachikawa_base/airfield", "kn8:defense_base/large_wings")) {
            helper.assertTrue(hq.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW).stream()
                    .anyMatch(info -> info.nbt() != null && info.nbt().getString("pool").equals(pool)),
                    "O QG de Tachikawa deveria levar a " + pool);
        }
        StructureTemplate training = manager.get(KN8Constants.id("tachikawa_base/training")).orElseThrow();
        var consoles = training.filterBlocks(BlockPos.ZERO, settings, KN8Blocks.TRAINING_CONSOLE.get());
        helper.assertTrue(consoles.size() == 1 && consoles.get(0).pos().getY() > 5,
                "O painel deveria ficar na sala de controle no alto");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, settings, Blocks.IRON_DOOR).size() == 4,
                "Duas comportas de ferro (porta de duas metades)");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, settings, Blocks.LEVER).size() == 2,
                "Duas alavancas na sala de controle");
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_WIRE).size() == 20,
                "Redstone das duas comportas (10 cada)");
        SiteDef base = KN8Data.SITE.server().get(KN8Constants.id("tachikawa_base"));
        helper.assertTrue(base.garrison().size() == 5 && base.garrisonAt().isPresent()
                && base.garrisonAt().get().piece().equals(KN8Constants.id("tachikawa_base/hq")),
                "A guarnicao de Tachikawa (com o Kafka, B-1) deveria aparecer no patio da base");
        helper.assertTrue(base.invasions().stream().anyMatch(entry -> entry.invasion()
                .equals(KN8Constants.id("tachikawa_raid"))), "O raide (No. 10) deveria cair na base");
        StructureTemplate station = manager.get(KN8Constants.id("tachikawa/station")).orElseThrow();
        helper.assertTrue(station.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW).stream()
                .noneMatch(info -> info.nbt() != null && info.nbt().getString("pool").contains("base")),
                "A estacao nao leva mais a base");
        assertAnchored(helper, "tachikawa_city", "tachikawa_base");
        assertAnchored(helper, "tachikawa_ward", "tachikawa_base");
        helper.succeed();
    }

    private static void assertAnchored(GameTestHelper helper, String district, String base) {
        Structure structure = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(KN8Constants.id(district));
        helper.assertTrue(structure instanceof CheckedJigsawStructure checked && checked.anchor()
                .map(anchor -> anchor.structure().is(KN8Constants.id(base))).orElse(false),
                district + " deveria nascer em volta da " + base);
    }

    /**
     * BASES-2 e BASES-4: a Base Maritima de Ariake e uma estrutura grande propria com o chao no y 12 e o subsolo
     * embaixo (abrigo e treino de trajes), a ala de seguranca com a sala de contencao, o porto grande a oeste com agua
     * e as alas; a guarnicao (Narumi, Isao) fica na base e a cidade costeira e os bairros em volta.
     */
    @GameTest(template = "empty_3x3")
    public static void ariakeBaseIsBigAndSurroundedByTheCity(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        var settings = new StructurePlaceSettings();
        StructureTemplate core = manager.get(KN8Constants.id("ariake_base/hq")).orElseThrow();
        var jigsaws = core.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW);
        int ground = jigsaws.get(0).pos().getY();
        helper.assertTrue(ground == 12, "O chao da base deveria ficar no y 12 (subsolo embaixo), esta no " + ground);
        for (String pool : List.of("kn8:ariake_base/front", "kn8:ariake_base/harbor", "kn8:defense_base/large_wings")) {
            helper.assertTrue(jigsaws.stream().anyMatch(info -> info.nbt() != null
                    && info.nbt().getString("pool").equals(pool)), "O QG de Ariake deveria levar a " + pool);
        }
        helper.assertTrue(core.filterBlocks(BlockPos.ZERO, settings, KN8Blocks.WEIGHT_RACK.get()).stream()
                .allMatch(info -> info.pos().getY() < ground), "O treino de trajes deveria ficar no subsolo");
        helper.assertTrue(core.filterBlocks(BlockPos.ZERO, settings, Blocks.REINFORCED_DEEPSLATE).size() > 500,
                "O abrigo fortificado deveria ter as paredes reforcadas");
        helper.assertTrue(core.filterBlocks(BlockPos.ZERO, settings, Blocks.TINTED_GLASS).size() > 20,
                "A sala de observacao da contencao deveria ter o vidro escuro");
        for (Block block : List.of(KN8Blocks.RESEARCH_TERMINAL.get(), KN8Blocks.SQUAD_STATION.get(),
                KN8Blocks.DEFENSE_WORKBENCH.get(), Blocks.IRON_DOOR, Blocks.LECTERN)) {
            helper.assertTrue(!core.filterBlocks(BlockPos.ZERO, settings, block).isEmpty(),
                    "A base de Ariake deveria ter " + block);
        }
        SiteDef base = KN8Data.SITE.server().get(KN8Constants.id("ariake_base"));
        helper.assertTrue(base != null && base.division().equals("first")
                && base.garrison().stream().anyMatch(member -> member.variant().equals("narumi"))
                && base.garrisonAt().map(at -> at.piece().equals(KN8Constants.id("ariake_base/hq"))).orElse(false),
                "Ariake: Primeira Divisao e a guarnicao dentro da base");
        helper.assertTrue(KN8Data.SITE.server().get(KN8Constants.id("ariake_city")).fieldMissions(),
                "Ariake: missoes de campo na cidade");
        assertAnchored(helper, "ariake_city", "ariake_base");
        assertAnchored(helper, "ariake_ward", "ariake_base");
        helper.succeed();
    }

    /**
     * BASES-3: a base de Tachikawa e a de Ariake tambem nascem na versao depois de um ataque (sorteada no pool):
     * vidros quebrados, crateras com a barreira amarela e preta, o posto de campanha com os servicos; o circuito das
     * comportas e o painel da arena continuam inteiros.
     */
    @GameTest(template = "empty_3x3")
    public static void basesHaveAPostAttackVariant(GameTestHelper helper) {
        var manager = helper.getLevel().getStructureManager();
        var settings = new StructurePlaceSettings();
        var pools = helper.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for (String city : List.of("tachikawa_base", "ariake_base")) {
            var pool = pools.get(KN8Constants.id(city + "/hq"));
            helper.assertTrue(pool != null && pool.getShuffledTemplates(helper.getLevel().getRandom()).stream()
                    .anyMatch(element -> element.toString().contains("hq_ruined")),
                    city + ": o pool da base deveria ter a versao atingida");
            StructureTemplate intact = manager.get(KN8Constants.id(city + "/hq")).orElseThrow();
            StructureTemplate ruined = manager.get(KN8Constants.id(city + "/hq_ruined")).orElseThrow();
            int glassIntact = glass(intact, settings);
            int glassRuined = glass(ruined, settings);
            helper.assertTrue(glassRuined < glassIntact * 0.6, city + ": vidros quebrados (" + glassRuined + " de "
                    + glassIntact + ")");
            // Zona interditada: barreira (onde os carros e o entulho deixam) e as luzes vermelhas de emergencia.
            helper.assertTrue(ruined.filterBlocks(BlockPos.ZERO, settings, Blocks.YELLOW_CONCRETE).size()
                    > intact.filterBlocks(BlockPos.ZERO, settings, Blocks.YELLOW_CONCRETE).size()
                    && ruined.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_LAMP).size()
                    >= intact.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_LAMP).size() + 3,
                    city + ": zonas interditadas com a barreira e as luzes de emergencia");
            for (Block block : List.of(KN8Blocks.RESEARCH_TERMINAL.get(), KN8Blocks.SQUAD_STATION.get(),
                    Blocks.WHITE_WOOL)) {
                helper.assertTrue(!ruined.filterBlocks(BlockPos.ZERO, settings, block).isEmpty(),
                        city + ": o posto de campanha deveria ter " + block);
            }
        }
        StructureTemplate training = manager.get(KN8Constants.id("tachikawa_base/training_ruined")).orElseThrow();
        helper.assertTrue(training.filterBlocks(BlockPos.ZERO, settings, Blocks.REDSTONE_WIRE).size() == 20
                && training.filterBlocks(BlockPos.ZERO, settings, KN8Blocks.TRAINING_CONSOLE.get()).size() == 1,
                "O campo de treino atingido continua com o painel e as comportas");
        helper.succeed();
    }

    private static int glass(StructureTemplate template, StructurePlaceSettings settings) {
        int count = 0;
        for (Block block : List.of(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS)) {
            count += template.filterBlocks(BlockPos.ZERO, settings, block).size();
        }
        return count;
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
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        StructureSet bases = sets.get(KN8Constants.id("defense_bases"));
        for (String base : List.of("tachikawa_base", "ariake_base")) {
            helper.assertTrue(bases != null && bases.structures().stream().anyMatch(entry -> entry.structure()
                    .is(KN8Constants.id(base))), "A base " + base + " deveria nascer no conjunto das bases");
        }
        for (String city : List.of("tachikawa", "ariake")) {
            StructureSet districts = sets.get(KN8Constants.id(city + "_districts"));
            helper.assertTrue(districts != null && districts.structures().size() == 2,
                    city + ": a cidade e os bairros deveriam estar no conjunto dos bairros");
        }
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(KN8Constants.id("defense_base_large")) == null, "A base grande generica nao nasce mais sozinha");
        helper.succeed();
    }

    /** No mundo plano e seco do teste o local passa na checagem e o desenho gera as pecas. */
    @GameTest(template = "empty_3x3", timeoutTicks = 200)
    public static void siteAcceptsFlatDryGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure site = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(KN8Constants.id("shinagawa_city"));
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        // Alguns chunks seguidos: parte cai perto de um ponto candidato de vila (planicie) ou de uma base (no plano toda
        // base cabe) e e recusada.
        ChunkPos base = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(4096, 0, 4096));
        boolean accepted = false;
        for (int i = 0; i < 64 && !accepted && site != null; i++) {
            ChunkPos chunk = new ChunkPos(base.x + i * 61, base.z - i * 47);
            Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                    generator.getBiomeSource(), level.getChunkSource().randomState(), level.getStructureManager(),
                    level.getSeed(), chunk, level, biome -> true);
            accepted = site.findValidGenerationPoint(context).isPresent();
        }
        helper.assertTrue(accepted, "No chao plano e seco a cidade de Shinagawa deveria passar na checagem do terreno");
        helper.succeed();
    }

    /**
     * BASES-4: a cidade de Tachikawa so pode nascer a 17 chunks ao norte do ponto candidato do conjunto das bases
     * (nunca um chunk ao lado nem no proprio chunk da base); e onde a ancora aceita, a base de Tachikawa cabe. No mundo
     * plano toda base cabe e a cidade sempre tem outra base a menos de 24 chunks: a cidade inteira so em jogo.
     */
    @GameTest(template = "empty_3x3", timeoutTicks = 600)
    public static void districtsOnlyAroundTheirBase(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure city = structures.get(KN8Constants.id("tachikawa_city"));
        StructureSet set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(KN8Constants.id("defense_bases"));
        helper.assertTrue(city instanceof CheckedJigsawStructure && set != null
                && set.placement() instanceof RandomSpreadStructurePlacement, "Cidade e conjunto das bases");
        RandomSpreadStructurePlacement spread = (RandomSpreadStructurePlacement) set.placement();
        var anchor = ((CheckedJigsawStructure) city).anchor().orElseThrow();
        helper.assertTrue(anchor.set().is(KN8Constants.id("defense_bases"))
                && anchor.structure().is(KN8Constants.id("tachikawa_base")), "A cidade se ancora na base de Tachikawa");
        ChunkPos start = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(8192, 0, 8192));
        for (int i = 0; i < 8; i++) {
            ChunkPos at = spread.getPotentialStructureChunk(level.getSeed(), start.x + i * spread.spacing(),
                    start.z);
            helper.assertTrue(CheckedJigsawStructure.anchorCandidateHere(context(level, new ChunkPos(at.x,
                    at.z - 17)), anchor), "17 chunks ao norte do ponto da base e lugar da cidade");
            helper.assertTrue(!CheckedJigsawStructure.anchorCandidateHere(context(level, new ChunkPos(at.x + 1,
                    at.z - 17)), anchor), "Um chunk ao lado nao e");
            helper.assertTrue(!CheckedJigsawStructure.anchorCandidateHere(context(level, at), anchor),
                    "O chunk da base nao e");
            ChunkPos north = new ChunkPos(at.x, at.z - 17);
            if (CheckedJigsawStructure.anchoredHere(context(level, north), anchor)) {
                helper.assertTrue(structures.get(KN8Constants.id("tachikawa_base"))
                        .findValidGenerationPoint(context(level, at)).isPresent(),
                        "Com a ancora aceita a base deveria caber");
            }
        }
        helper.succeed();
    }

    private static Structure.GenerationContext context(ServerLevel level, ChunkPos chunk) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), chunk, level,
                biome -> true);
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
