package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.kn8.KN8Constants;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.registry.KN8Entities;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-7: provas de promocao com metricas reais (precisao, dano recebido, aliados perdidos, esquadrao
 * minimo), avaliacao uma vez por conclusao, reprovacao sem recompensa com espera, e saves antigos sem as medidas.
 * Um perfil de jogador por teste (os testes do lote rodam em paralelo).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExamGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation PRIMIGENIUS = KN8Constants.id("primigenius");
    private static final ResourceLocation MARKSMAN = KN8Constants.id("exam_marksman");
    private static final ResourceLocation COMMAND = KN8Constants.id("exam_command");

    private ExamGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_exam:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_exam_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4, 1, 4)));
        return player;
    }

    /** PM-8B: prova so se aceita com o Oficial de Provas perto. */
    private static void examOfficer(GameTestHelper helper) {
        DefenseOfficerEntity officer = helper.spawn(KN8Entities.DEFENSE_OFFICER.get(), new BlockPos(5, 1, 4));
        officer.setGiver(DefenseOfficerEntity.EXAM_OFFICER);
    }

    private static void start(GameTestHelper helper, FakePlayer player, ResourceLocation id, int objectives) {
        CareerService.data(player).active().put(id, new CareerData.MissionProgress(
                new ArrayList<>(Collections.nCopies(objectives, 0)), helper.getLevel().getGameTime(),
                new ArrayList<>()));
    }

    private static void shots(FakePlayer player, int total, int hits) {
        for (int i = 0; i < total; i++) {
            MissionService.onShot(player, i < hits);
        }
    }

    private static String rank(FakePlayer player) {
        return CareerService.rank(player).map(e -> e.getKey().getPath()).orElse("?");
    }

    @GameTest(template = TEMPLATE)
    public static void marksmanExamFailsOnAimAndPassesOnce(GameTestHelper helper) {
        FakePlayer player = player(helper, "marksman");
        examOfficer(helper);
        CareerService.setRank(player, KN8Constants.id("oficial"));
        CareerData data = CareerService.data(player);
        helper.assertTrue(MissionService.canAccept(player, MARKSMAN) == MissionService.Result.OK,
                "Oficial deveria poder aceitar a Prova de Tiro");

        // Reprovada: 20 tiros, 4 acertos (20% < 35%).
        start(helper, player, MARKSMAN, 1);
        shots(player, 20, 4);
        int merit = data.merit();
        for (int i = 0; i < 3; i++) {
            CareerService.onKaijuKilled(player, PRIMIGENIUS, KaijuClass.YOJU);
        }
        helper.assertFalse(data.completed().contains(MARKSMAN), "Prova com 20% de precisao nao pode concluir");
        helper.assertFalse(data.active().containsKey(MARKSMAN), "Prova reprovada deveria sair das ativas");
        helper.assertTrue(data.merit() == merit + 3 * 25,
                "Reprovada nao da a recompensa (so o merito dos abates); veio " + (data.merit() - merit));
        helper.assertTrue(MissionService.canAccept(player, MARKSMAN) == MissionService.Result.COOLDOWN,
                "Reprovada so pode ser tentada de novo depois da espera");
        helper.assertTrue(data.shotsFired() == 20 && data.shotsHit() == 4, "Precisao da carreira nao contou");

        // Aprovada: espera vencida, 20 tiros com 12 acertos (60%).
        data.cooldowns().remove(MARKSMAN);
        start(helper, player, MARKSMAN, 1);
        shots(player, 20, 12);
        merit = data.merit();
        for (int i = 0; i < 3; i++) {
            CareerService.onKaijuKilled(player, PRIMIGENIUS, KaijuClass.YOJU);
        }
        helper.assertTrue(data.completed().contains(MARKSMAN), "Prova com 60% deveria concluir");
        helper.assertTrue(data.merit() >= merit + 3 * 25 + 150, "Recompensa da prova nao chegou");
        helper.assertTrue(data.bestGrades().containsKey(MARKSMAN), "A nota deveria ficar guardada");
        helper.assertTrue(MissionService.canAccept(player, MARKSMAN) == MissionService.Result.ALREADY_DONE,
                "Prova aprovada nao pode ser repetida (recompensa uma vez so)");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void higherRanksNeedTheirExam(GameTestHelper helper) {
        FakePlayer player = player(helper, "ranks");
        examOfficer(helper);
        CareerService.setRank(player, KN8Constants.id("oficial"));
        CareerService.addMerit(player, 20_000);
        helper.assertTrue(rank(player).equals("oficial"), "Sem a Prova de Tiro deveria ficar Oficial, veio "
                + rank(player));
        CareerService.data(player).completed().add(MARKSMAN);
        CareerService.data(player).completed().add(KN8Constants.id("exam_squad"));
        CareerService.tryPromote(player);
        helper.assertTrue(rank(player).equals("lider_pelotao"),
                "Com duas provas deveria parar em Lider de Pelotao, veio " + rank(player));
        // Esquadrao minimo: sem soldados a Prova de Comando e recusada.
        helper.assertTrue(MissionService.canAccept(player, COMMAND) == MissionService.Result.SQUAD_TOO_SMALL,
                "Prova de Comando sem esquadrao deveria ser recusada");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missionStatsSaveAndOldSavesLoad(GameTestHelper helper) {
        FakePlayer player = player(helper, "save");
        CareerData data = CareerService.data(player);
        start(helper, player, KN8Constants.id("extermination"), 1);
        shots(player, 7, 3);
        MissionService.onDamageTaken(player, 4.5F);
        MissionService.onDamageDealt(player, 120.0F);
        MissionService.onAllyLost(player);
        Tag saved = CareerData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        CareerData loaded = CareerData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        CareerData.MissionStats stats = loaded.active().get(KN8Constants.id("extermination")).stats();
        helper.assertTrue(stats.shots == 7 && stats.hits == 3 && stats.alliesLost == 1
                && Math.abs(stats.damageTaken - 4.5F) < 0.01F && Math.abs(stats.damageDealt - 120.0F) < 0.01F,
                "Medidas da missao nao voltaram do save");
        helper.assertTrue(loaded.shotsFired() == 7 && loaded.shotsHit() == 3, "Precisao da carreira nao voltou");

        // Save de antes da PM-7: missao sem "stats" e carreira sem os campos novos.
        CompoundTag old = new CompoundTag();
        CompoundTag active = new CompoundTag();
        CompoundTag mission = new CompoundTag();
        mission.put("progress", NbtOps.INSTANCE.createIntList(IntStream.of(1)));
        mission.putLong("accepted", 5L);
        active.put("kn8:extermination", mission);
        old.put("active", active);
        old.putInt("merit", 42);
        CareerData legacy = CareerData.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
        CareerData.MissionProgress progress = legacy.active().get(KN8Constants.id("extermination"));
        helper.assertTrue(legacy.merit() == 42 && progress != null && progress.stats().shots == 0
                && List.of(1).equals(progress.progress()), "Save antigo deveria carregar com medidas zeradas");
        helper.succeed();
    }
}
