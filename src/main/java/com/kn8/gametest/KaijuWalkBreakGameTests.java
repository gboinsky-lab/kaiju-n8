// src/main/java/com/kn8/gametest/KaijuWalkBreakGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTest da 0.6 (Miguel: kaiju presos em construcoes): andando ate um alvo atras de uma parede de tijolo de pedra
 * (categoria "resistente"), o Yoju abre passagem com a forca de andar do config ({@code walkPowerYoju} 3).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuWalkBreakGameTests {

    private static final int WALL_Z = 5;
    private static final int TIMEOUT = 200;

    private KaijuWalkBreakGameTests() {
    }

    @GameTest(template = "empty_9x7x9", timeoutTicks = TIMEOUT)
    public static void yojuBreaksAStoneBrickWallWhileWalking(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int y = 1; y < 6; y++) {
                helper.setBlock(new BlockPos(x, y, WALL_Z), Blocks.STONE_BRICKS);
            }
        }
        KaijuEntity brute = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 2));
        brute.setYRot(0.0F);
        brute.setYBodyRot(0.0F);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 7));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> brute.setTarget(golem));
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, WALL_Z)).isAir(),
                "O kaiju deveria quebrar a parede de tijolo de pedra para chegar ao alvo"));
    }
}
