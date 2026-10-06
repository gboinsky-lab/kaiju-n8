package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.boss.BossService;
import com.kn8.common.career.CareerData;
import com.kn8.common.career.CareerService;
import com.kn8.common.career.MissionService;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.world.KaijuSpawner;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.2 (Etapas 2, 5 e 6): promocao por merito + missao de avaliacao, teto de Release 100 para todos,
 * limite do boneco de treino, missao que faz kaiju surgirem e conta abates, e chefe que troca de fase e invoca Yoju
 * da propria especie. Um perfil de jogador por teste (os testes do lote rodam em paralelo).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CareerGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final ResourceLocation PRIMIGENIUS = KN8Constants.id("primigenius");
    private static final ResourceLocation EXAM = KN8Constants.id("exam");
    private static final ResourceLocation EXTERMINATION = KN8Constants.id("extermination");

    private CareerGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_career:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 1, 4)));
        return player;
    }

    @GameTest(template = TEMPLATE)
    public static void promotionNeedsMeritAndExam(GameTestHelper helper) {
        FakePlayer player = player(helper, "promotion");
        helper.assertTrue(CareerService.rank(player).map(e -> e.getKey().getPath()).orElse("").equals("candidato"),
                "Jogador novo deveria ser Candidato");
        CareerService.addMerit(player, 20_000);
        helper.assertTrue(CareerService.rank(player).map(e -> e.getKey().getPath()).orElse("").equals("candidato"),
                "Sem o Exame de Admissao nao pode virar Oficial, mesmo com merito");
        CareerService.data(player).completed().add(EXAM);
        CareerService.tryPromote(player);
        helper.assertTrue(CareerService.rank(player).map(e -> e.getKey().getPath()).orElse("").equals("capitao"),
                "Com o exame e 20000 de merito deveria subir ate Capitao, veio "
                        + CareerService.rank(player).map(e -> e.getKey().getPath()).orElse("?"));
        helper.assertTrue(Math.abs(player.getMaxHealth() - 30.0F) < 0.01F,
                "Capitao deveria ter 30 de vida maxima, veio " + player.getMaxHealth());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void releaseTrainsToHundredForEveryone(GameTestHelper helper) {
        FakePlayer player = player(helper, "release");
        helper.assertTrue(PowerService.cap(player) == 100, "Teto deveria ser 100 para todos (patente nao limita)");
        PowerService.addTrainingXp(player, 1_000_000);
        helper.assertTrue(PowerService.data(player).trainedRelease() == 100,
                "Treino suficiente deveria levar o Release a 100%, veio " + PowerService.data(player).trainedRelease());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dummyXpIsLimitedPerMinute(GameTestHelper helper) {
        FakePlayer player = player(helper, "dummy");
        int total = 0;
        for (int i = 0; i < 40; i++) {
            total += CareerService.onDummyHit(player);
        }
        helper.assertTrue(total == 60, "40 golpes no mesmo minuto deveriam dar so 60 XP, deram " + total);
        helper.succeed();
    }

    /**
     * Abates contam para o objetivo atual e a recompensa chega. A missao e posta direto no estado (aceitar faria os
     * kaiju surgirem a 40-80 blocos, em cima de outros testes do lote).
     */
    @GameTest(template = TEMPLATE)
    public static void missionCountsKillsAndRewards(GameTestHelper helper) {
        FakePlayer player = player(helper, "mission");
        helper.assertTrue(MissionService.canAccept(player, EXTERMINATION) == MissionService.Result.OK,
                "Candidato deveria poder aceitar o Exterminio");
        CareerService.data(player).active().put(EXTERMINATION, new CareerData.MissionProgress(
                new ArrayList<>(List.of(0)), helper.getLevel().getGameTime(), new ArrayList<>()));
        int meritBefore = CareerService.data(player).merit();
        CareerService.onKaijuKilled(player, PRIMIGENIUS, KaijuClass.YOJU);
        helper.assertTrue(CareerService.data(player).active().get(EXTERMINATION).progress().get(0) == 1,
                "O primeiro abate deveria contar 1/2");
        CareerService.onKaijuKilled(player, PRIMIGENIUS, KaijuClass.YOJU);
        helper.assertTrue(CareerService.data(player).completed().contains(EXTERMINATION),
                "Dois abates deveriam concluir o Exterminio");
        helper.assertTrue(CareerService.data(player).merit() >= meritBefore + 60,
                "Recompensa de merito da missao nao chegou");
        helper.assertTrue(MissionService.canAccept(player, EXTERMINATION) == MissionService.Result.COOLDOWN,
                "Missao repetivel deveria entrar em espera");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void spawnerPutsKaijuOnTheGround(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(4, 3, 4));
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("trichonephila"), at)
                .orElse(null);
        helper.assertTrue(kaiju != null && kaiju.isPersistenceRequired(), "O kaiju do evento deveria surgir e ficar");
        helper.assertTrue(kaiju.getY() < at.getY(), "O kaiju deveria descer ate o chao");
        kaiju.discard();
        helper.succeed();
    }

    /** Lote proprio: o chefe (9 m) e os Primigenius invocados ocupariam a area dos testes vizinhos. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200, batch = "kn8_boss")
    public static void bossChangesPhaseAndSummonsOwnSpecies(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity boss = BossService.spawn(helper.getLevel(), KN8Constants.id("honju"), at).orElse(null);
        helper.assertTrue(boss != null && boss.bossState() != null, "O chefe kn8:honju deveria surgir como chefe");
        helper.runAfterDelay(5, () -> {
            int before = helper.getLevel().getEntitiesOfClass(KaijuEntity.class, boss.getBoundingBox().inflate(80),
                    kaiju -> kaiju.kaijuId().equals(PRIMIGENIUS)).size();
            boss.setHealth(boss.getMaxHealth() * 0.4F);
            BossService.tick(boss, boss.bossState());
            List<KaijuEntity> minions = helper.getLevel().getEntitiesOfClass(KaijuEntity.class,
                    boss.getBoundingBox().inflate(80), kaiju -> kaiju.kaijuId().equals(PRIMIGENIUS));
            helper.assertTrue(boss.bossState().phase() == 1, "Com 40% de vida o chefe deveria estar na fase 2");
            helper.assertTrue(minions.size() > before, "A troca de fase deveria invocar Primigenius");
            helper.assertTrue(!boss.hurt(helper.getLevel().damageSources().generic(), 5.0F),
                    "Logo apos trocar de fase o chefe deveria estar invulneravel");
            minions.forEach(KaijuEntity::discard);
            boss.discard();
            helper.succeed();
        });
    }
}
