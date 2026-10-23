package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.evacuation.CivilianEntity;
import com.kn8.common.evacuation.EvacuationService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-10A: evacuacao. Os civis surgem uma vez ao aceitar (com o ponto de extracao e a ameaca), cada
 * chegada conta, cada perda conta e perdas demais fracassam a missao; no fim os civis que sobraram somem. O civil
 * salvo e carregado guarda dono, missao, ponto e variante; jogadores nao o ferem. O FakePlayer nao esta na lista de
 * jogadores, entao as chegadas/perdas sao contadas direto pelo {@link MissionService} (o caminho pelo civil foi
 * conferido em jogo). Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EvacuationGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation DISTRICT = KN8Constants.id("evacuation_district");

    private EvacuationGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_evac:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_evac_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.data(player).active().clear();
        CareerService.data(player).cooldowns().clear();
        CareerService.data(player).completed().remove(DISTRICT);
        CareerService.setRank(player, KN8Constants.id("candidato"));
        return player;
    }

    /** Some com a ameaca da missao (Primigenius perto do jogador). */
    private static void clearThreat(ServerLevel level, Player player) {
        level.getEntitiesOfClass(KaijuEntity.class, new AABB(player.blockPosition()).inflate(96)).forEach(
                KaijuEntity::discard);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_evacuation")
    public static void evacuationCountsArrivalsLossesAndFails(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "fail");
        helper.assertTrue(MissionService.accept(player, DISTRICT) == MissionService.Result.OK, "Missao nao aceita");
        clearThreat(level, player);
        List<CivilianEntity> civilians = EvacuationService.civilians(level, player.getUUID(), DISTRICT);
        helper.assertTrue(civilians.size() == 5, "4 civis + 1 de folga deveriam surgir, vieram " + civilians.size());
        BlockPos extraction = civilians.get(0).extraction();
        helper.assertTrue(extraction != null && civilians.stream().allMatch(c -> extraction.equals(c.extraction())),
                "Todos os civis deveriam conhecer o mesmo ponto de extracao");
        CareerData.MissionProgress progress = CareerService.data(player).active().get(DISTRICT);
        helper.assertTrue(extraction.equals(progress.points().get(0)), "O ponto de extracao vai para a HUD");
        double dx = extraction.getX() - civilians.get(0).getX();
        double dz = extraction.getZ() - civilians.get(0).getZ();
        helper.assertTrue(Math.sqrt(dx * dx + dz * dz) >= 20, "O ponto de extracao deveria ficar longe dos civis");

        helper.assertTrue(MissionService.onCivilianEvacuated(player, DISTRICT), "Chegada deveria contar");
        helper.assertTrue(progress.progress().get(0) == 1 && progress.stats().civiliansSaved == 1,
                "Uma chegada = 1 evacuado");
        MissionService.onCivilianLost(player, DISTRICT);
        helper.assertTrue(MissionService.isActive(player, DISTRICT) && progress.stats().civiliansLost == 1,
                "Uma perda dentro do permitido nao fracassa");
        MissionService.onCivilianLost(player, DISTRICT);
        helper.assertFalse(MissionService.isActive(player, DISTRICT), "Perdas acima do permitido fracassam");
        helper.assertTrue(EvacuationService.civilians(level, player.getUUID(), DISTRICT).isEmpty(),
                "No fim da missao os civis que sobraram somem");
        helper.assertFalse(MissionService.onCivilianEvacuated(player, DISTRICT),
                "Sem missao ativa a chegada nao conta");
        helper.assertFalse(CareerService.data(player).completed().contains(DISTRICT), "Fracassada nao conclui");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_evacuation")
    public static void evacuationCompletesAndRemovesTheSpare(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "complete");
        int meritBefore = CareerService.data(player).merit();
        helper.assertTrue(MissionService.accept(player, DISTRICT) == MissionService.Result.OK, "Missao nao aceita");
        clearThreat(level, player);
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(MissionService.onCivilianEvacuated(player, DISTRICT), "Chegada " + i + " nao contou");
        }
        helper.assertFalse(MissionService.isActive(player, DISTRICT), "4 evacuados concluem a missao");
        helper.assertTrue(CareerService.data(player).completed().contains(DISTRICT), "Missao concluida");
        helper.assertTrue(CareerService.data(player).merit() >= meritBefore + 90, "Recompensa da missao");
        helper.assertTrue(EvacuationService.civilians(level, player.getUUID(), DISTRICT).isEmpty(),
                "O civil de folga some no fim");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void civilianSavesAndIgnoresFriendlyFire(GameTestHelper helper) {
        FakePlayer player = player(helper, "save");
        CivilianEntity civilian = helper.spawn(KN8Entities.CIVILIAN.get(), new BlockPos(4, 1, 4));
        BlockPos point = helper.absolutePos(new BlockPos(1, 1, 1));
        civilian.assign(player.getUUID(), DISTRICT, point);
        civilian.setVariant(2);
        CompoundTag tag = new CompoundTag();
        civilian.saveWithoutId(tag);
        CivilianEntity loaded = KN8Entities.CIVILIAN.get().create(helper.getLevel());
        loaded.load(tag);
        helper.assertTrue(player.getUUID().equals(loaded.owner()) && DISTRICT.equals(loaded.mission())
                && point.equals(loaded.extraction()) && loaded.variant() == 2, "Civil salvo perdeu os dados");
        float health = civilian.getHealth();
        civilian.hurt(player.damageSources().playerAttack(player), 6.0F);
        helper.assertTrue(civilian.getHealth() == health, "Jogador nao fere civil");
        civilian.hurt(player.damageSources().generic(), 6.0F);
        helper.assertTrue(civilian.getHealth() < health, "O resto fere");
        civilian.discard();
        helper.succeed();
    }
}
