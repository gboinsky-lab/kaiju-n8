package com.kn8.gametest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.attribute.PowerService;
import com.kn8.common.config.ServerConfig;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.training.SparringService;
import com.kn8.common.training.WeightTraining;
import com.kn8.core.power.BodyStat;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da TREINO (Miguel, 2026-10-30): o corpo se treina por exercicio. Bater em kaiju nao treina a forca; o
 * suporte de pesos treina com stamina, intervalo e limite por minuto; o treino de combate com soldado deixa os dois se
 * ferirem sem ninguem morrer, treina a forca e da o bonus a quem vence. O FakePlayer e invulneravel: serve so como
 * quem bate (o soldado batendo no jogador fica para o teste em jogo). Um perfil por teste.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrainingGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private TrainingGameTests() {
    }

    private static FakePlayer player(GameTestHelper helper, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(("kn8_training:" + name).getBytes(
                StandardCharsets.UTF_8)), "kn8_t_" + name);
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), profile);
        player.moveTo(helper.absoluteVec(new Vec3(4, 1, 4)));
        PowerService.setBodyLevel(player, BodyStat.STRENGTH, 0);
        player.getPersistentData().remove(WeightTraining.TAG);
        player.getPersistentData().remove(SparringService.TAG);
        return player;
    }

    /** Total de XP de forca (nivel convertido de volta nao importa: so se subiu). */
    private static long strength(FakePlayer player) {
        return PowerService.bodyLevel(player, BodyStat.STRENGTH) * 100_000L
                + PowerService.data(player).bodyXp(BodyStat.STRENGTH);
    }

    @GameTest(template = TEMPLATE, batch = "kn8_training_kaiju")
    public static void hittingKaijuDoesNotTrainTheBody(GameTestHelper helper) {
        FakePlayer player = player(helper, "kaiju");
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(6, 1, 6));
        kaiju.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            long before = strength(player);
            kaiju.hurt(player.damageSources().playerAttack(player), 10.0F);
            helper.assertTrue(kaiju.getHealth() < kaiju.getMaxHealth(), "o golpe deveria ferir o kaiju");
            helper.assertTrue(strength(player) == before, "golpe em kaiju nao deveria treinar a forca");
            kaiju.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_training")
    public static void weightRackTrainsStrengthWithLimits(GameTestHelper helper) {
        FakePlayer player = player(helper, "lift");
        PowerService.setStamina(player, 1000.0);
        long before = strength(player);
        helper.assertTrue(WeightTraining.lift(player) == WeightTraining.Result.LIFTED, "primeira repeticao");
        helper.assertTrue(strength(player) > before, "repeticao deveria treinar a forca");
        helper.assertTrue(WeightTraining.lift(player) == WeightTraining.Result.TOO_FAST, "repeticao cedo demais");
        int interval = ServerConfig.LIFT_INTERVAL_TICKS.get();
        helper.runAfterDelay(interval + 1, () -> {
            PowerService.setStamina(player, 0.0);
            helper.assertTrue(WeightTraining.lift(player) == WeightTraining.Result.NO_STAMINA, "sem stamina");
        });
        helper.runAfterDelay(2L * interval + 3, () -> {
            PowerService.setStamina(player, 1000.0);
            CompoundTag data = player.getPersistentData().getCompound(WeightTraining.TAG);
            data.putInt("reps", ServerConfig.LIFT_REPS_PER_MINUTE.get());
            player.getPersistentData().put(WeightTraining.TAG, data);
            long full = strength(player);
            helper.assertTrue(WeightTraining.lift(player) == WeightTraining.Result.TIRED, "limite do minuto");
            helper.assertTrue(strength(player) == full, "cansado nao treina");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_training")
    public static void sparringIsNonLethalAndRewardsTheWinner(GameTestHelper helper) {
        FakePlayer player = player(helper, "spar");
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(5, 1, 4));
        helper.runAfterDelay(2, () -> {
            float full = soldier.getMaxHealth();
            helper.assertFalse(soldier.hurt(player.damageSources().playerAttack(player), 2.0F),
                    "fora do treino o soldado nao leva golpe do jogador");
            SparringService.toggle(player, soldier);
            helper.assertTrue(SparringService.isPartner(player, soldier), "treino deveria comecar");
            helper.assertTrue(SoldierEntity.isDuelFoe(soldier, player), "no treino o soldado pode bater no jogador");
            long before = strength(player);
            helper.assertTrue(soldier.hurt(player.damageSources().playerAttack(player), 2.0F),
                    "no treino o golpe do jogador fere");
            helper.assertTrue(strength(player) > before, "bater no parceiro treina a forca");
            soldier.invulnerableTime = 0;
            soldier.hurt(player.damageSources().playerAttack(player), 1000.0F);
            helper.assertTrue(soldier.isAlive(), "ninguem morre no treino");
            helper.assertFalse(SparringService.isPartner(player, soldier), "quem caiu ao limite perde: treino acaba");
            helper.assertTrue(soldier.getHealth() == full, "o soldado sai do treino curado");
            helper.assertFalse(player.getPersistentData().hasUUID(SparringService.TAG), "marca do jogador some");
            SparringService.toggle(player, soldier);
            helper.assertFalse(SparringService.isPartner(player, soldier), "o soldado descansa antes do proximo");
            soldier.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_training_kaiju2")
    public static void sparringRefusedWithKaijuNearby(GameTestHelper helper) {
        FakePlayer player = player(helper, "spar_kaiju");
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(5, 1, 4));
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(2, 1, 7));
        kaiju.setNoAi(true);
        soldier.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            SparringService.toggle(player, soldier);
            helper.assertFalse(SparringService.isPartner(player, soldier), "kaiju por perto: sem treino");
            kaiju.discard();
            soldier.discard();
            helper.succeed();
        });
    }
}
