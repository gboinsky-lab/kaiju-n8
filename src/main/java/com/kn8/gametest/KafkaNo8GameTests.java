// src/main/java/com/kn8/gametest/KafkaNo8GameTests.java
package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;
import com.kn8.common.soldier.special.KafkaEntity;
import com.kn8.common.soldier.special.KaijuNo8Entity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.7-F: o Kafka vira o Kaiju No. 8 quando a vida cai abaixo da metade, o No. 8 regenera abaixo da metade,
 * volta a ser o Kafka depois de um tempo sem alvo e o soco pesado acerta.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KafkaNo8GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final int SETTLE = 3;

    private KafkaNo8GameTests() {
    }

    private static <T extends Entity> List<T> around(GameTestHelper helper, Class<T> type) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        return helper.getLevel().getEntitiesOfClass(type, box);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void kafkaTransformsWhenLosing(GameTestHelper helper) {
        KafkaEntity kafka = helper.spawn(KN8Entities.KAFKA.get(), new BlockPos(4, 1, 4));
        kafka.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(kafka.getMainHandItem().is(KN8Items.RIFLE.get()), "Kafka com o rifle");
            kafka.setHealth(kafka.getMaxHealth() * 0.3F);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(kafka.isRemoved(), "O Kafka deveria sumir na transformacao");
            List<KaijuNo8Entity> no8 = around(helper, KaijuNo8Entity.class);
            helper.assertTrue(!no8.isEmpty() && no8.get(0).getHealth() > 1000.0F,
                    "O Kaiju No. 8 deveria surgir com a vida cheia");
            no8.forEach(HoshinaEntity::discard);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void no8RegeneratesBelowHalf(GameTestHelper helper) {
        KaijuNo8Entity no8 = helper.spawn(KN8Entities.KAIJU_NO8.get(), new BlockPos(4, 1, 4));
        no8.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            no8.setHealth(no8.getMaxHealth() * 0.3F);
            start[0] = no8.getHealth();
        });
        helper.runAfterDelay(SETTLE + 45, () -> {
            helper.assertTrue(no8.getHealth() > start[0] + 50.0F, "Deveria regenerar: " + start[0] + " -> "
                    + no8.getHealth());
            no8.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void no8RevertsToKafkaWhenIdle(GameTestHelper helper) {
        KaijuNo8Entity no8 = helper.spawn(KN8Entities.KAIJU_NO8.get(), new BlockPos(4, 1, 4));
        no8.setNoAi(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(no8.isRemoved(), "Sem alvo, o No. 8 deveria voltar a ser o Kafka");
            List<KafkaEntity> kafka = around(helper, KafkaEntity.class);
            helper.assertTrue(!kafka.isEmpty(), "O Kafka deveria voltar no lugar");
            kafka.forEach(HoshinaEntity::discard);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void no8HeavyPunchHits(GameTestHelper helper) {
        KaijuNo8Entity no8 = helper.spawn(KN8Entities.KAIJU_NO8.get(), new BlockPos(3, 1, 4));
        no8.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 4));
        golem.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            start[0] = golem.getHealth();
            no8.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(no8.startTechnique("heavy_punch", golem), "Soco pesado comecou");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            helper.assertTrue(golem.getHealth() < start[0], "O golem levou o soco: " + golem.getHealth());
            no8.discard();
            helper.succeed();
        });
    }
}
