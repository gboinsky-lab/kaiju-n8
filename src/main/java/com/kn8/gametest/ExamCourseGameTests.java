package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.base.DefenseOfficerEntity;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.destruction.ProtectedAreas;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.core.power.BodyStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-8B: provas (cursos) diferentes das missoes. So se aceitam com o Oficial de Provas perto, acontecem
 * onde foram aceitas (kaiju marcados perto, area protegida), contam so os kaiju delas, nao deixam carcaca, fortalecem
 * o corpo e reprovam quem sai da base. Um perfil de jogador por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExamCourseGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation STRENGTH = KN8Constants.id("course_strength");
    private static final ResourceLocation AGILITY = KN8Constants.id("course_agility");
    private static final ResourceLocation PRIMIGENIUS = KN8Constants.id("primigenius");

    private ExamCourseGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_course:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_course_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));
        CareerService.data(player).active().clear();
        CareerService.data(player).cooldowns().clear();
        CareerService.setRank(player, KN8Constants.id("candidato"));
        return player;
    }

    private static DefenseOfficerEntity officer(GameTestHelper helper, ResourceLocation giver) {
        DefenseOfficerEntity officer = helper.spawn(KN8Entities.DEFENSE_OFFICER.get(), new BlockPos(5, 1, 4));
        officer.setGiver(giver);
        return officer;
    }

    private static List<KaijuEntity> examKaiju(ServerLevel level, FakePlayer player, BlockPos origin) {
        return level.getEntitiesOfClass(KaijuEntity.class, new AABB(origin).inflate(48),
                kaiju -> ExamTargets.isExam(kaiju) && player.getUUID().equals(ExamTargets.owner(kaiju))
                        && kaiju.isAlive());
    }

    private static void forceChunks(ServerLevel level, BlockPos center, boolean forced) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setChunkForced((center.getX() >> 4) + dx, (center.getZ() >> 4) + dz, forced);
            }
        }
    }

    @GameTest(template = TEMPLATE)
    public static void examNeedsTheExamOfficer(GameTestHelper helper) {
        FakePlayer player = player(helper, "officer");
        helper.assertTrue(MissionService.canAccept(player, STRENGTH) == MissionService.Result.NEEDS_EXAM_OFFICER,
                "Sem oficial a prova deveria ser recusada");
        DefenseOfficerEntity officer = officer(helper, DefenseOfficerEntity.OFFICER);
        helper.assertTrue(MissionService.canAccept(player, STRENGTH) == MissionService.Result.NEEDS_EXAM_OFFICER,
                "O Oficial de Missoes nao aplica prova");
        officer.setGiver(DefenseOfficerEntity.EXAM_OFFICER);
        helper.assertTrue(MissionService.canAccept(player, STRENGTH) == MissionService.Result.OK,
                "Com o Oficial de Provas perto a prova deveria ser aceita");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_exam_course")
    public static void courseRunsInsideTheBaseAndStrengthensTheBody(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "strength");
        officer(helper, DefenseOfficerEntity.EXAM_OFFICER);
        BlockPos center = player.blockPosition();
        forceChunks(level, center, true);
        CareerData data = CareerService.data(player);
        int levelBefore = PowerService.bodyLevel(player, BodyStat.STRENGTH);
        int xpBefore = PowerService.data(player).bodyXp(BodyStat.STRENGTH);
        int meritBefore = data.merit();
        helper.assertTrue(MissionService.accept(player, STRENGTH) == MissionService.Result.OK, "Prova nao aceita");
        CareerData.MissionProgress progress = data.active().get(STRENGTH);
        helper.assertTrue(progress != null && progress.origin().isPresent()
                && progress.origin().get().equals(center), "A prova deveria guardar o lugar onde foi aceita");
        helper.assertTrue(ProtectedAreas.get(level).isProtected(center), "A area da prova deveria ficar protegida");

        helper.runAfterDelay(3, () -> {
            List<KaijuEntity> kaiju = examKaiju(level, player, center);
            helper.assertTrue(kaiju.size() == 2, "A prova deveria trazer 2 kaiju marcados, veio " + kaiju.size());
            for (KaijuEntity one : kaiju) {
                double dx = one.getX() - center.getX();
                double dz = one.getZ() - center.getZ();
                helper.assertTrue(Math.sqrt(dx * dx + dz * dz) <= 20, "Kaiju da prova longe demais da base");
            }
            // Abate de campo nao conta para a prova.
            CareerService.onKaijuKilled(player, PRIMIGENIUS, KaijuClass.YOJU);
            helper.assertTrue(data.active().get(STRENGTH).progress().get(0) == 0,
                    "Kaiju de fora nao pode contar para a prova");
            meritCheck(helper, data, meritBefore + 25);
            kaiju.get(0).kill();
        });
        helper.runAfterDelay(60, () -> {
            // O abate pelo evento de morte acha o dono na lista de jogadores (o FakePlayer nao esta nela: conferido
            // em jogo); aqui conta direto pelo MissionService.
            MissionService.onExamKill(player, PRIMIGENIUS, java.util.Optional.empty());
            helper.assertTrue(data.active().get(STRENGTH).progress().get(0) == 1,
                    "O kaiju da prova abatido deveria contar");
            meritCheck(helper, data, meritBefore + 25);
            helper.assertTrue(level.getEntitiesOfClass(CarcassEntity.class, new AABB(center).inflate(48)).isEmpty(),
                    "Kaiju da prova nao deixa carcaca");
            MissionService.onExamKill(player, PRIMIGENIUS, java.util.Optional.empty());
        });
        helper.runAfterDelay(65, () -> {
            helper.assertFalse(data.active().containsKey(STRENGTH), "A prova deveria terminar");
            int levelAfter = PowerService.bodyLevel(player, BodyStat.STRENGTH);
            int xpAfter = PowerService.data(player).bodyXp(BodyStat.STRENGTH);
            helper.assertTrue(levelAfter > levelBefore || xpAfter > xpBefore, "A prova deveria dar XP de forca");
            helper.assertFalse(ProtectedAreas.get(level).areas().containsKey(ExamTargets.areaName(player.getUUID(),
                    STRENGTH)), "A area deveria ser liberada no fim");
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(examKaiju(level, player, center).isEmpty(), "Kaiju da prova deveriam sumir no fim");
            forceChunks(level, center, false);
            helper.succeed();
        });
    }

    private static void meritCheck(GameTestHelper helper, CareerData data, int expected) {
        helper.assertTrue(data.merit() == expected, "Merito esperado " + expected + ", veio " + data.merit());
    }

    @GameTest(template = TEMPLATE)
    public static void leavingTheBaseFailsTheExam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = player(helper, "leave");
        officer(helper, DefenseOfficerEntity.EXAM_OFFICER);
        BlockPos center = player.blockPosition();
        helper.assertTrue(MissionService.accept(player, AGILITY) == MissionService.Result.OK, "Curso nao aceito");
        MissionService.tick(player);
        helper.assertTrue(CareerService.data(player).active().containsKey(AGILITY), "Dentro da base continua");
        player.moveTo(Vec3.atCenterOf(center).add(80, 0, 0));
        MissionService.tick(player);
        helper.assertFalse(CareerService.data(player).active().containsKey(AGILITY),
                "Sair da base deveria reprovar a prova");
        helper.assertFalse(ProtectedAreas.get(level).areas().containsKey(ExamTargets.areaName(player.getUUID(),
                AGILITY)), "A area deveria ser liberada");
        helper.succeed();
    }
}
