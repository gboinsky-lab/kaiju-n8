package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.ExamTargets;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.trail.KaijuTrail;
import com.kn8.common.trail.KaijuTrailService;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-9: rastro de destruicao. Pistas espacadas por kaiju, sem pista de kaiju de prova, investigacao que
 * da merito uma vez por pista, prazo e save.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrailGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private TrailGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_trail")
    public static void trailRecordsSpacedCluesAndInvestigationPaysOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        KaijuTrail.get(level).points().clear();
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 4));
        kaiju.setNoAi(true);
        KaijuEntity exam = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(2, 1, 2));
        exam.setNoAi(true);
        ExamTargets.mark(exam, UUID.randomUUID(), KN8Constants.id("course_strength"));
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(
                "kn8_trail".getBytes(StandardCharsets.UTF_8)), "kn8_trail"));
        player.moveTo(helper.absoluteVec(new Vec3(4.5, 1, 4.5)));

        KaijuTrailService.note(kaiju, KaijuTrail.Kind.DESTRUCTION);
        KaijuTrailService.note(kaiju, KaijuTrail.Kind.DESTRUCTION);
        helper.assertTrue(KaijuTrailService.count(level, kaiju.getUUID()) == 1, "Pistas no mesmo lugar nao repetem");
        kaiju.moveTo(kaiju.position().add(ServerConfig.TRAIL_POINT_SPACING.get() + 2, 0, 0));
        KaijuTrailService.note(kaiju, KaijuTrail.Kind.DESTRUCTION);
        helper.assertTrue(KaijuTrailService.count(level, kaiju.getUUID()) == 2, "Pista nova depois de andar");
        KaijuTrailService.note(exam, KaijuTrail.Kind.DESTRUCTION);
        helper.assertTrue(KaijuTrailService.count(level, exam.getUUID()) == 0, "Kaiju de prova nao deixa rastro");

        int merit = CareerService.data(player).merit();
        KaijuTrailService.Investigation first = KaijuTrailService.investigate(player);
        helper.assertTrue(first.trails() == 1 && first.clues() == 2 && first.fresh() == 2,
                "Investigacao deveria achar 1 rastro com 2 pistas novas: " + first);
        int expected = merit + 2 * ServerConfig.TRAIL_MERIT_PER_CLUE.get();
        helper.assertTrue(CareerService.data(player).merit() == expected, "Merito das pistas novas");
        KaijuTrailService.Investigation second = KaijuTrailService.investigate(player);
        helper.assertTrue(second.fresh() == 0 && CareerService.data(player).merit() == expected,
                "A mesma pista nao paga duas vezes");

        CompoundTag tag = KaijuTrail.get(level).save(new CompoundTag(), level.registryAccess());
        KaijuTrail loaded = KaijuTrail.load(tag, level.registryAccess());
        helper.assertTrue(loaded.points().size() == 2 && loaded.points().get(0).investigated()
                && kaiju.getUUID().equals(loaded.points().get(1).kaiju()), "Rastro nao voltou do save");
        int removed = KaijuTrail.get(level).expire(level.getGameTime() + ServerConfig.TRAIL_LIFE_TICKS.get(),
                ServerConfig.TRAIL_LIFE_TICKS.get());
        helper.assertTrue(removed == 2 && KaijuTrail.get(level).points().isEmpty(), "Pistas vencidas saem");
        kaiju.discard();
        exam.discard();
        helper.succeed();
    }
}
