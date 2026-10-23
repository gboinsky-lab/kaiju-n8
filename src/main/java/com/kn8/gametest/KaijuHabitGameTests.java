// src/main/java/com/kn8/gametest/KaijuHabitGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-3 (IA que le habitos): o kaiju inteligente percebe o alvo que fica longe e o que repete o mesmo
 * ataque e muda de jeito; o kaiju comum nao le nada.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuHabitGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 2;
    /** min_samples 8 x sample_ticks 10 do kaiju_ai, com folga. */
    private static final int READ_TICKS = 100;

    private KaijuHabitGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140, batch = "kn8_habits_distance")
    public static void smartKaijuReadsATargetThatKeepsDistance(GameTestHelper helper) {
        KaijuEntity smart = helper.spawn(KN8Entities.KAIJU_NO10_SMALL.get(), new BlockPos(2, 1, 2));
        smart.setNoAi(true);
        KaijuEntity common = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(2, 1, 12));
        common.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(13, 1, 13));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(smart.isSmart(), "O No. 10 deveria ser inteligente");
            helper.assertTrue(!common.isSmart(), "O Primigenius nao e inteligente");
            smart.setTarget(golem);
            common.setTarget(golem);
        });
        helper.runAfterDelay(SETTLE + READ_TICKS, () -> {
            helper.assertTrue(smart.edgeDistance(golem) > 10.0, "O alvo deveria estar longe ("
                    + smart.edgeDistance(golem) + ")");
            helper.assertTrue(smart.activeHabit().filter("keeps_distance"::equals).isPresent(),
                    "O No. 10 deveria ler que o alvo fica longe, leu " + smart.activeHabit());
            helper.assertTrue(smart.habitResponse().map(r -> r.speed() > 1.0F).orElse(false),
                    "A resposta deveria acelerar a aproximacao");
            helper.assertTrue(common.activeHabit().isEmpty(), "O kaiju comum nao le habitos");
            smart.discard();
            common.discard();
            golem.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80, batch = "kn8_habits_repeat")
    public static void smartKaijuAnswersARepeatedAttack(GameTestHelper helper) {
        KaijuEntity smart = helper.spawn(KN8Entities.KAIJU_NO10_SMALL.get(), new BlockPos(7, 1, 5));
        smart.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 9));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> smart.setTarget(golem));
        for (int i = 0; i < 4; i++) {
            int hit = i;
            helper.runAfterDelay(SETTLE + 2 + hit * 5, () -> {
                smart.invulnerableTime = 0;
                smart.hurt(helper.getLevel().damageSources().mobAttack(golem), 3.0F);
            });
        }
        helper.runAfterDelay(SETTLE + 2 + 3 * 5 + 1, () -> {
            helper.assertTrue(smart.activeHabit().filter("repeats"::equals).isPresent(),
                    "Quatro golpes iguais seguidos deveriam virar habito, leu " + smart.activeHabit());
            helper.assertTrue(smart.habitResponse().map(r -> r.backOff() > 0.0F).orElse(false),
                    "A resposta ao golpe repetido abre distancia");
            smart.discard();
            golem.discard();
            helper.succeed();
        });
    }
}
