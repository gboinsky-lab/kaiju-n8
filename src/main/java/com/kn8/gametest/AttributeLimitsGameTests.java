// src/main/java/com/kn8/gametest/AttributeLimitsGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 0.6-D: vida maxima acima do teto vanilla de 1024 (kaiju de fortitude alta e chefes; ver AttributeLimits). */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AttributeLimitsGameTests {

    private static final double BIG_HEALTH = 2560.0;

    private AttributeLimitsGameTests() {
    }

    @GameTest(template = "empty_9x7x9")
    public static void maxHealthGoesAbove1024(GameTestHelper helper) {
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(4, 1, 4));
        cow.setNoAi(true);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BIG_HEALTH);
        cow.setHealth(cow.getMaxHealth());
        helper.assertTrue(cow.getMaxHealth() == (float) BIG_HEALTH, "Vida maxima deveria passar de 1024: "
                + cow.getMaxHealth());
        helper.assertTrue(cow.getHealth() == (float) BIG_HEALTH, "Vida cheia deveria ser 2560: " + cow.getHealth());
        helper.succeed();
    }
}
