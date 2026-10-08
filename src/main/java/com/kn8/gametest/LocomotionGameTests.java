// src/main/java/com/kn8/gametest/LocomotionGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.anim.Locomotion;
import com.kn8.common.data.KN8Data;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.soldier.SoldierEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 0.5.0-C: perfis de locomocao carregados e passos pela passada. */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LocomotionGameTests {

    private LocomotionGameTests() {
    }

    @GameTest(template = "empty_9x7x9")
    public static void everyKaijuHasALocomotionProfile(GameTestHelper helper) {
        KN8Data.KAIJU.server().keySet().forEach(id -> helper.assertTrue(KN8Data.LOCOMOTION.server().containsKey(id),
                "Kaiju sem perfil de locomocao: " + id));
        helper.assertTrue(KN8Data.LOCOMOTION.server().get(KN8Constants.id("kaiju_no10_giant")).stride()
                > KN8Data.LOCOMOTION.server().get(KN8Constants.id("kaiju_no10_small")).stride(),
                "A forma gigante deveria ter passada maior que a pequena");
        helper.succeed();
    }

    @GameTest(template = "empty_9x7x9")
    public static void soldierStepsFollowTheStride(GameTestHelper helper) {
        SoldierEntity soldier = helper.spawn(KN8Entities.SOLDIER.get(), new BlockPos(4, 1, 4));
        soldier.moveDist = 10.0F;
        float stepEvery = KN8Data.LOCOMOTION.server().get(KN8Constants.id("soldier")).stepEvery();
        float next = Locomotion.nextStep(soldier, Locomotion.typeId(soldier), 11.0F);
        helper.assertTrue(Math.abs(next - (10.0F + stepEvery * 0.6F)) < 1.0E-4F,
                "Proximo passo deveria vir em meia passada, veio " + next);
        soldier.discard();
        helper.succeed();
    }
}
