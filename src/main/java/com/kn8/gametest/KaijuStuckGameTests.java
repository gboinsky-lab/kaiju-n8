// src/main/java/com/kn8/gametest/KaijuStuckGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests das correcoes de 2026-10-16 (Miguel): soldados nao ficam presos em corpos de kaiju; kaiju nao ficam
 * presos em cima de construcoes (descem quebrando o chao construido; troca de forma nasce no lugar).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuStuckGameTests {

    private static final String TEMPLATE = "empty_15x12x15";

    private KaijuStuckGameTests() {
    }

    /** Carcaca nao e mais solida para soldado (comum e especial) nem para kaiju; o jogador continua batendo nela. */
    @GameTest(template = TEMPLATE, batch = "kn8_stuck_carcass", timeoutTicks = 40)
    public static void carcassDoesNotBlockSoldiers(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 7));
        kaiju.setNoAi(true);
        helper.runAfterDelay(3, () -> {
            CarcassEntity carcass = CarcassEntity.from(kaiju).orElseThrow();
            helper.getLevel().addFreshEntity(carcass);
            kaiju.discard();
            SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(7, 1, 7));
            KaijuEntity other = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 1, 7));
            helper.assertTrue(!soldier.canCollideWith(carcass) && !other.canCollideWith(carcass),
                    "Soldado e kaiju nao deveriam colidir com a carcaca");
            helper.assertTrue(helper.getLevel().noCollision(soldier),
                    "O soldado dentro do corpo do kaiju deveria poder sair (sem colisao com a carcaca)");
            carcass.discard();
            soldier.discard();
            other.discard();
            helper.succeed();
        });
    }

    /** Kaiju em cima de uma laje de tijolo com o alvo embaixo dela: quebra o chao construido e desce. */
    @GameTest(template = TEMPLATE, batch = "kn8_stuck_sink", timeoutTicks = 300)
    public static void kaijuSinksThroughBuiltFloor(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        for (int x = 0; x < 15; x++) {
            for (int z = 0; z < 15; z++) {
                for (int y = 1; y <= 3; y++) {
                    boolean pocket = x >= 6 && x <= 8 && z >= 6 && z <= 8 && y <= 2;
                    helper.setBlock(new BlockPos(x, y, z), pocket ? Blocks.AIR : Blocks.STONE_BRICKS);
                }
            }
        }
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(7, 1, 7));
        cow.setNoAi(true);
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(7, 4, 7));
        double[] start = new double[1];
        helper.runAfterDelay(5, () -> {
            start[0] = kaiju.getY();
            kaiju.setTarget(cow);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(start[0] != 0.0 && kaiju.getY() < start[0] - 1.5,
                    "O kaiju deveria quebrar a laje e descer ate o alvo (y " + kaiju.getY() + ", inicio " + start[0]
                            + ")");
            kaiju.discard();
            cow.discard();
        });
    }

    /** Troca de forma nasce no lugar (quebra o que ocupa o corpo) em vez de subir para cima da construcao. */
    @GameTest(template = TEMPLATE, batch = "kn8_stuck_in_place", timeoutTicks = 40)
    public static void formChangeSpawnsInPlace(GameTestHelper helper) {
        helper.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, helper.getLevel().getServer());
        for (int x = 4; x <= 10; x++) {
            for (int z = 4; z <= 10; z++) {
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE_BRICKS);
                }
            }
        }
        BlockPos at = helper.absolutePos(new BlockPos(7, 1, 7));
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("primigenius"), at, true)
                .orElseThrow();
        helper.assertTrue(Math.abs(kaiju.getY() - at.getY()) < 0.01,
                "A forma nova deveria ficar na altura do ponto (y " + kaiju.getY() + ", ponto " + at.getY()
                        + "), nao em cima da construcao");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(kaiju.getY() < at.getY() + 1.0, "Deveria continuar embaixo, quebrando a construcao");
            kaiju.discard();
            helper.succeed();
        });
    }
}
