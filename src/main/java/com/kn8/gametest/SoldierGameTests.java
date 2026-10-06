// src/main/java/com/kn8/gametest/SoldierGameTests.java
package com.kn8.gametest;

import java.util.UUID;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da Etapa F (0.1-B): soldado com rifle acerta um kaiju a mais de 20 blocos; niveis de potencia escalam o
 * dano pela formula do Release; fogo amigo (jogador) nao fere o soldado.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoldierGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final int SHOOT_TIMEOUT = 200;
    private static final GameProfile PROFILE =
            new GameProfile(UUID.fromString("5e4d3c2b-1a09-4f8e-9d7c-6b5a4f3e2d1c"), "kn8_ally");

    private SoldierGameTests() {
    }

    private static SoldierEntity soldier(GameTestHelper helper, BlockPos pos, String variant, String level) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), pos);
        soldier.setVariant(variant);
        soldier.setPowerLevel(level);
        soldier.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(soldier.blockPosition()),
                MobSpawnType.COMMAND, null);
        return soldier;
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = SHOOT_TIMEOUT)
    public static void riflemanHitsAKaijuFromAfar(GameTestHelper helper) {
        KaijuEntity brute = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 3));
        brute.setNoAi(true);
        SoldierEntity rifleman = soldier(helper, new BlockPos(4, 1, 29), "rifle", "normal");
        helper.runAfterDelay(2, () -> {
            float before = brute.getHealth();
            rifleman.setTarget(brute);
            helper.succeedWhen(() -> helper.assertTrue(brute.getHealth() < before,
                    "O soldado com rifle deveria acertar o kaiju de longe"));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void powerLevelsScaleWithRelease(GameTestHelper helper) {
        SoldierEntity low = soldier(helper, new BlockPos(2, 1, 4), "knife", "low");
        SoldierEntity elite = soldier(helper, new BlockPos(6, 1, 4), "knife", "elite");
        helper.assertTrue(elite.release() > low.release(), "Elite deveria ter mais Release que o nivel baixo");
        helper.assertTrue(elite.damageMultiplier() > low.damageMultiplier(), "Elite deveria causar mais dano");
        low.discard();
        elite.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playersCannotHurtSoldiers(GameTestHelper helper) {
        SoldierEntity ally = soldier(helper, new BlockPos(4, 1, 4), "rifle", "normal");
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), PROFILE);
        float before = ally.getHealth();
        ally.hurt(helper.getLevel().damageSources().playerAttack(player), 10.0F);
        helper.assertTrue(ally.getHealth() == before, "Golpe de jogador nao deveria ferir o soldado aliado");
        helper.assertTrue(!ally.canAttack(player), "Soldado nunca mira em jogadores");
        ally.discard();
        helper.succeed();
    }
}
