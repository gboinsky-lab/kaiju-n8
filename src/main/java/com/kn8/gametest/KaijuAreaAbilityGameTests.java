// src/main/java/com/kn8/gametest/KaijuAreaAbilityGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M11a: o golpe no chao (kn8:area_melee, slam do Primigenius) atinge quem esta no raio do JSON a frente
 * do kaiju e mais ninguem; a investida (kn8:charge) leva o kaiju ate o alvo e acerta cada alvo uma unica vez.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuAreaAbilityGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final int SLAM_IMPACT_MARGIN = 24;
    private static final int CHARGE_TIMEOUT = 120;
    private static final float TOLERANCE = 0.1F;

    private KaijuAreaAbilityGameTests() {
    }

    private static KaijuEntity facingSouth(GameTestHelper helper, BlockPos pos, boolean ai) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), pos);
        kaiju.setNoAi(!ai);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        return kaiju;
    }

    @GameTest(template = TEMPLATE)
    public static void slamHitsOnlyInsideTheRadius(GameTestHelper helper) {
        KaijuEntity brute = facingSouth(helper, new BlockPos(4, 1, 1), false);
        Cow near = helper.spawn(EntityType.COW, new BlockPos(4, 1, 5));
        Cow far = helper.spawn(EntityType.COW, new BlockPos(4, 1, 8));
        near.setNoAi(true);
        far.setNoAi(true);
        // [perto, longe]: passos no nivel de cima com atraso absoluto (sem runAfterDelay aninhado).
        float[] before = new float[2];
        helper.runAfterDelay(2, () -> {
            before[0] = near.getHealth();
            before[1] = far.getHealth();
            helper.assertTrue(brute.startAbility(KN8Constants.id("slam"), near), "O slam deveria comecar");
        });
        helper.runAfterDelay(2 + SLAM_IMPACT_MARGIN, () -> {
            helper.assertTrue(near.isDeadOrDying() || near.getHealth() < before[0],
                    "A vaca dentro do raio deveria levar o golpe");
            helper.assertTrue(far.getHealth() == before[1], "A vaca fora do raio nao deveria levar dano");
            brute.discard();
            near.discard();
            far.discard();
            helper.succeed();
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = CHARGE_TIMEOUT)
    public static void chargeReachesTheTargetAndHitsOnce(GameTestHelper helper) {
        KaijuEntity brute = facingSouth(helper, new BlockPos(4, 1, 2), true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 14));
        golem.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            float before = golem.getHealth();
            float oneHit = (float) (brute.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2);
            helper.assertTrue(brute.startAbility(KN8Constants.id("charge"), golem), "A investida deveria comecar");
            helper.succeedWhen(() -> {
                helper.assertTrue(!brute.isUsingAbility(), "A investida ainda nao terminou");
                helper.assertTrue(golem.getHealth() < before, "A investida deveria acertar o golem");
                // Uma unica vez (com folga para a armadura natural do golem, que e zero).
                helper.assertTrue(Math.abs(golem.getHealth() - (before - oneHit)) < TOLERANCE,
                        "O golem deveria levar um acerto so: esperado " + (before - oneHit) + ", veio "
                                + golem.getHealth());
                brute.discard();
                golem.discard();
            });
        });
    }
}
