package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.MissionDef;
import com.kn8.common.data.def.SiteDef;
import com.kn8.common.site.SiteService;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da POS-ATAQUE: as missoes pos-ataque so sao aceitas dentro de uma base que nasceu atingida (BASES-3), e o
 * servidor reconhece a versao atingida pelas pecas da estrutura gerada. A missao na base de verdade so pelo teste em
 * jogo (o GameTest nao tem o local gerado).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PostAttackGameTests {

    private static final List<String> MISSIONS = List.of("post_attack_rescue", "post_attack_sweep",
            "post_attack_recovery");
    /** Sorteios do pool do QG (inteiro 3 : atingido 1): com 32 a chance de nao sair nenhum dos dois e desprezivel. */
    private static final int DRAWS = 32;

    private PostAttackGameTests() {
    }

    @GameTest(template = "empty_3x3")
    public static void postAttackMissionsNeedAnAttackedBase(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("kn8_post_attack".getBytes(
                StandardCharsets.UTF_8)), "kn8_post_attack");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
        CareerService.data(player).active().clear();
        CareerService.data(player).cooldowns().clear();
        CareerService.promote(player, KN8Constants.id("oficial"));
        for (String id : MISSIONS) {
            MissionDef def = KN8Data.MISSION.server().get(KN8Constants.id(id));
            helper.assertTrue(def != null && def.arena().map(MissionDef.Arena::ruined).orElse(false),
                    id + " deveria pedir uma base atingida");
            helper.assertTrue(def.giver().map(giver -> giver.equals(KN8Constants.id("officer"))).orElse(false),
                    id + " deveria ser do Oficial de Missoes (o do posto de campanha)");
            MissionService.Result result = MissionService.canAccept(player, KN8Constants.id(id));
            helper.assertTrue(result == MissionService.Result.NEEDS_RUINED_BASE,
                    id + " fora de uma base atingida deveria ser recusada: " + result);
        }
        helper.assertTrue(MissionService.canAccept(player, KN8Constants.id("extermination"))
                == MissionService.Result.OK, "Missao comum continua aceita em qualquer lugar");
        helper.succeed();
    }

    /** As bases geradas saem inteiras ou atingidas pelo pool, e {@code Hit.ruined} distingue as duas. */
    @GameTest(template = "empty_3x3", timeoutTicks = 400)
    public static void attackedBaseIsRecognized(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure layout = structures.get(KN8Constants.id("tachikawa_base_layout"));
        SiteDef site = KN8Data.SITE.server().get(KN8Constants.id("tachikawa_base"));
        helper.assertTrue(layout != null && site != null, "Desenho e local da base de Tachikawa");
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos base = new ChunkPos(helper.absolutePos(BlockPos.ZERO).offset(16384, 0, 16384));
        int ruined = 0;
        int intact = 0;
        for (int i = 0; i < DRAWS; i++) {
            StructureStart start = layout.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
                    new ChunkPos(base.x + i * 7, base.z), 0, level, biome -> true);
            if (!start.isValid()) {
                continue;
            }
            if (new SiteService.Hit(KN8Constants.id("tachikawa_base"), site, start).ruined()) {
                ruined++;
            } else {
                intact++;
            }
        }
        helper.assertTrue(ruined > 0 && intact > 0, "Esperava bases inteiras e atingidas: " + intact + " inteiras, "
                + ruined + " atingidas");
        helper.succeed();
    }
}
