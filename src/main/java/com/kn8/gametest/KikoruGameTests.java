// src/main/java/com/kn8/gametest/KikoruGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.KikoruEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da Kikoru (0.5.0-D8): perfil (machado de duas maos, vida, aura amarela e Release do JSON) e uma tecnica
 * corpo a corpo (Heavy Swing) que acerta no tick do JSON.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KikoruGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final int SETTLE = 3;

    private KikoruGameTests() {
    }

    private static KikoruEntity kikoru(GameTestHelper helper, BlockPos pos) {
        KikoruEntity kikoru = helper.spawn(KN8Entities.KIKORU.get(), pos);
        kikoru.setNoAi(true);
        return kikoru;
    }

    @GameTest(template = TEMPLATE)
    public static void kikoruProfileGivesTheAxeAndAura(GameTestHelper helper) {
        KikoruEntity kikoru = kikoru(helper, new BlockPos(4, 1, 4));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kikoru.getMainHandItem().is(KN8Items.AXE.get()), "Machado na mao direita");
            helper.assertTrue(kikoru.getOffhandItem().isEmpty(), "Mao esquerda livre (machado de duas maos)");
            SpecialSoldierDef profile = kikoru.profile().orElseThrow();
            helper.assertTrue(kikoru.getAttributeValue(Attributes.MAX_HEALTH) == profile.health(),
                    "Vida do perfil (" + profile.health() + "): " + kikoru.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(kikoru.getData(KN8Attachments.AURA).equals(KN8Constants.id("kikoru_lightning")),
                    "Aura amarela: " + kikoru.getData(KN8Attachments.AURA));
            helper.assertTrue(kikoru.getData(KN8Attachments.RELEASE_VISUAL) == profile.release(),
                    "Release do perfil: " + kikoru.getData(KN8Attachments.RELEASE_VISUAL));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void kikoruHeavySwingHitsOnTheJsonTick(GameTestHelper helper) {
        KikoruEntity kikoru = kikoru(helper, new BlockPos(3, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 4));
        golem.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            start[0] = golem.getHealth();
            kikoru.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(kikoru.startTechnique("heavy_swing", golem), "Heavy Swing comecou");
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(golem.getHealth() < start[0], "O golem levou o golpe: " + golem.getHealth());
            helper.succeed();
        });
    }
}
