package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.evacuation.EvacuationService;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.support.SupportService;
import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-12 (integracao final do Plano Mestre, Parte VI): o que as etapas testaram cada uma no seu canto,
 * aqui junto. Dois jogadores na mesma missao recebem cada um a sua avaliacao e a recompensa uma vez so; a missao de
 * evacuacao que estoura o prazo fracassa e leva os civis junto; chamar o apoio de novo no mesmo instante e recusado
 * sem gastar; a carreira com tudo o que as etapas PM acrescentaram (pesquisa, dominio, notas, medidas da missao) sai
 * e volta do save igual. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntegrationGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation EXTERMINATION = KN8Constants.id("extermination");
    private static final ResourceLocation PRIMIGENIUS = KN8Constants.id("primigenius");
    private static final ResourceLocation DISTRICT = KN8Constants.id("evacuation_district");
    private static final ResourceLocation PATROL = KN8Constants.id("patrol");

    private IntegrationGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_integration:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_int_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerData data = CareerService.data(player);
        data.active().clear();
        data.cooldowns().clear();
        data.completed().clear();
        data.bestGrades().clear();
        CareerService.setRank(player, KN8Constants.id("candidato"));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void twoPlayersSameMissionEachGradedOnce(GameTestHelper helper) {
        FakePlayer first = player(helper, "first");
        FakePlayer second = player(helper, "second");
        long now = helper.getLevel().getGameTime();
        for (FakePlayer player : List.of(first, second)) {
            CareerService.data(player).active().put(EXTERMINATION, new CareerData.MissionProgress(
                    new ArrayList<>(List.of(0)), now, new ArrayList<>()));
        }
        int firstMerit = CareerService.data(first).merit();
        int secondMerit = CareerService.data(second).merit();
        // O mesmo abate contado nos dois (cada um derrubou o seu); so o primeiro jogador conclui agora.
        CareerService.onKaijuKilled(first, PRIMIGENIUS, KaijuClass.YOJU);
        CareerService.onKaijuKilled(first, PRIMIGENIUS, KaijuClass.YOJU);
        CareerService.onKaijuKilled(second, PRIMIGENIUS, KaijuClass.YOJU);
        helper.assertTrue(CareerService.data(first).completed().contains(EXTERMINATION), "O primeiro concluiu");
        helper.assertTrue(MissionService.isActive(second, EXTERMINATION), "O segundo ainda tem 1/2: nada vazou");
        int afterFirst = CareerService.data(first).merit();
        String grade = CareerService.data(first).bestGrades().get(EXTERMINATION);
        helper.assertTrue(grade != null, "Uma avaliacao para o primeiro");
        // Abates depois de concluida nao dao nova recompensa nem nova avaliacao.
        CareerService.onKaijuKilled(first, PRIMIGENIUS, KaijuClass.YOJU);
        CareerService.onKaijuKilled(first, PRIMIGENIUS, KaijuClass.YOJU);
        int extra = CareerService.data(first).merit() - afterFirst;
        helper.assertTrue(extra == 2 * ServerConfig.MERIT_KILL_YOJU.get(),
                "Depois de concluida so o merito do abate solto entra, sem a recompensa da missao: " + extra);
        helper.assertTrue(grade.equals(CareerService.data(first).bestGrades().get(EXTERMINATION)),
                "A avaliacao nao se repete");
        helper.assertTrue(CareerService.data(second).merit() > secondMerit && afterFirst > firstMerit,
                "Cada um com o proprio merito");
        CareerService.onKaijuKilled(second, PRIMIGENIUS, KaijuClass.YOJU);
        helper.assertTrue(CareerService.data(second).completed().contains(EXTERMINATION)
                && CareerService.data(second).bestGrades().get(EXTERMINATION) != null,
                "O segundo conclui com a propria avaliacao");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100, batch = "kn8_integration_evac")
    public static void evacuationTimesOutAndClearsCivilians(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "timeout");
        helper.assertTrue(MissionService.accept(player, DISTRICT) == MissionService.Result.OK, "Missao nao aceita");
        level.getEntitiesOfClass(KaijuEntity.class, new AABB(player.blockPosition()).inflate(96))
                .forEach(KaijuEntity::discard);
        helper.assertFalse(EvacuationService.civilians(level, player.getUUID(), DISTRICT).isEmpty(),
                "Civis da missao no mundo");
        CareerData.MissionProgress progress = CareerService.data(player).active().get(DISTRICT);
        // Aceita ha mais tempo que o prazo do JSON (9.600 ticks): o tick da missao fracassa por tempo.
        CareerService.data(player).active().put(DISTRICT, new CareerData.MissionProgress(progress.progress(),
                level.getGameTime() - 10_000, progress.points(), progress.stats(), progress.origin()));
        MissionService.tick(player);
        helper.assertFalse(MissionService.isActive(player, DISTRICT), "Prazo estourado fracassa a missao");
        helper.assertTrue(EvacuationService.civilians(level, player.getUUID(), DISTRICT).isEmpty(),
                "Os civis somem com a missao que fracassou");
        helper.assertFalse(CareerService.data(player).completed().contains(DISTRICT), "Fracassada nao conclui");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "kn8_support")
    public static void supportSpamIsRefusedWithoutSpending(GameTestHelper helper) {
        FakePlayer player = player(helper, "spam");
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove("kn8_support");
        CareerService.setRank(player, KN8Constants.id("oficial"));
        helper.assertTrue(MissionService.accept(player, PATROL) == MissionService.Result.OK, "Patrulha nao aceita");
        player.setXRot(90.0F);
        String op = SupportService.operation(player).orElseThrow();
        SupportService.Result first = SupportService.call(player, SupportService.Tier.SUPPLY);
        SupportService.Result second = SupportService.call(player, SupportService.Tier.SUPPLY);
        helper.assertTrue(first == SupportService.Result.OK, "Primeira chamada pelo radio: " + first);
        helper.assertTrue(second == SupportService.Result.TOO_FAST, "Segunda no mesmo instante: " + second);
        helper.assertTrue(SupportService.used(player, SupportService.Tier.SUPPLY, op) == 1,
                "A chamada recusada nao gasta");
        MissionService.abandon(player, PATROL);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void careerWithEveryPlanFieldSurvivesSave(GameTestHelper helper) {
        FakePlayer player = player(helper, "save");
        CareerData data = CareerService.data(player);
        ResourceLocation research = KN8Constants.id("yoju_tissue");
        ResourceLocation gear = KN8Constants.id("numbers2");
        data.research().add(research);
        data.mastery().put(gear, 1234);
        data.bestGrades().put(EXTERMINATION, "A");
        CareerData.MissionProgress progress = new CareerData.MissionProgress(new ArrayList<>(List.of(1)), 77,
                new ArrayList<>());
        progress.stats().civiliansSaved = 3;
        progress.stats().civiliansLost = 1;
        data.active().put(DISTRICT, progress);
        Tag saved = CareerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        CareerData loaded = CareerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.research().contains(research), "Pesquisa concluida volta");
        helper.assertTrue(loaded.mastery().getOrDefault(gear, 0) == 1234, "Dominio volta");
        helper.assertTrue("A".equals(loaded.bestGrades().get(EXTERMINATION)), "Melhor nota volta");
        CareerData.MissionProgress back = loaded.active().get(DISTRICT);
        helper.assertTrue(back != null && back.acceptedTick() == 77 && back.progress().get(0) == 1,
                "Missao ativa volta com o progresso");
        helper.assertTrue(back.stats().civiliansSaved == 3 && back.stats().civiliansLost == 1,
                "Medidas da evacuacao voltam");
        data.active().clear();
        data.mastery().clear();
        data.research().clear();
        helper.succeed();
    }
}
