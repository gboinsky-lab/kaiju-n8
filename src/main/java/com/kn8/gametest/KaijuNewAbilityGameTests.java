// src/main/java/com/kn8/gametest/KaijuNewAbilityGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.6-A (ataques novos dos kaiju): rabada (kn8:sweep) so atras e dos lados, projeteis (Finger Gun do
 * No. 9 e teia da Trichonephila, que deixa lento), combo de pernas (kn8:multi_hit, varios golpes), salto
 * (kn8:leap, dano na aterrissagem) e furia (rage do JSON) com a vida baixa.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuNewAbilityGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final int SETTLE = 2;
    private static final int TIMEOUT = 140;

    private KaijuNewAbilityGameTests() {
    }

    private static KaijuEntity facingSouth(GameTestHelper helper,
            EntityType<KaijuEntity> type, BlockPos pos) {
        KaijuEntity kaiju = helper.spawn(type, pos);
        kaiju.setNoAi(true);
        kaiju.setYRot(0.0F);
        kaiju.setYBodyRot(0.0F);
        kaiju.yBodyRotO = 0.0F;
        return kaiju;
    }

    private static <T extends LivingEntity> T still(T entity) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
        }
        return entity;
    }

    @GameTest(template = TEMPLATE)
    public static void tailSwipeHitsBehindButNotInFront(GameTestHelper helper) {
        KaijuEntity brute = facingSouth(helper, KN8Entities.PRIMIGENIUS.get(), new BlockPos(4, 1, 4));
        Cow behind = still(helper.spawn(EntityType.COW, new BlockPos(4, 1, 0)));
        Cow front = still(helper.spawn(EntityType.COW, new BlockPos(4, 1, 8)));
        float[] before = new float[2];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = behind.getHealth();
            before[1] = front.getHealth();
            helper.assertTrue(brute.startAbility(KN8Constants.id("tail_swipe"), behind), "A rabada deveria comecar");
        });
        helper.runAfterDelay(SETTLE + 20, () -> {
            helper.assertTrue(behind.isDeadOrDying() || behind.getHealth() < before[0],
                    "A vaca atras deveria levar a rabada");
            helper.assertTrue(front.getHealth() == before[1], "A vaca na frente nao deveria levar a rabada");
            helper.succeed();
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void fingerGunHitsADistantTarget(GameTestHelper helper) {
        KaijuEntity no9 = facingSouth(helper, KN8Entities.KAIJU_NO9.get(), new BlockPos(4, 1, 2));
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 20)));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(no9.startAbility(KN8Constants.id("finger_gun"), golem), "O Finger Gun deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < before[0],
                    "O projetil deveria atingir o golem a 18 blocos"));
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void webShotSlowsTheTarget(GameTestHelper helper) {
        KaijuEntity spider = facingSouth(helper, KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 2));
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 14)));
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(spider.startAbility(KN8Constants.id("web_shot"), golem), "A teia deveria comecar");
            helper.succeedWhen(() -> helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "A teia deveria deixar o alvo lento"));
        });
    }

    @GameTest(template = TEMPLATE)
    public static void multiLegHitsSeveralTimes(GameTestHelper helper) {
        KaijuEntity spider = facingSouth(helper, KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 2));
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 5)));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(spider.startAbility(KN8Constants.id("multi_leg"), golem), "O combo deveria comecar");
        });
        helper.runAfterDelay(SETTLE + 30, () -> {
            float oneHit = (float) (spider.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5);
            float lost = before[0] - golem.getHealth();
            helper.assertTrue(lost > oneHit * 3.5F, "Os 4 golpes do combo deveriam acertar: perdeu " + lost
                    + ", um golpe = " + oneHit);
            helper.succeed();
        });
    }

    @GameTest(template = LONG_TEMPLATE, timeoutTicks = TIMEOUT)
    public static void leapLandsOnTheTarget(GameTestHelper helper) {
        KaijuEntity spider = facingSouth(helper, KN8Entities.TRICHONEPHILA.get(), new BlockPos(4, 1, 2));
        // Com a IA desligada o mob nao se move pela velocidade: o salto precisa da IA ligada.
        spider.setNoAi(false);
        IronGolem golem = still(helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 12)));
        float[] before = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            before[0] = golem.getHealth();
            helper.assertTrue(spider.startAbility(KN8Constants.id("leap"), golem), "O salto deveria comecar");
            helper.succeedWhen(() -> {
                helper.assertTrue(spider.getZ() > helper.absoluteVec(new Vec3(0, 0, 7)).z,
                        "A aranha deveria ter saltado ate perto do alvo");
                helper.assertTrue(golem.getHealth() < before[0], "A aterrissagem deveria ferir o alvo");
            });
        });
    }

    @GameTest(template = TEMPLATE)
    public static void rageStartsBelowTheHealthThreshold(GameTestHelper helper) {
        KaijuEntity revived = facingSouth(helper, KN8Entities.PRIMIGENIUS_RESURRECTED.get(), new BlockPos(4, 1, 4));
        double[] damage = new double[1];
        helper.runAfterDelay(SETTLE, () -> {
            damage[0] = revived.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(!revived.isEnraged(), "Com vida cheia nao deveria estar enfurecido");
            revived.setHealth(revived.getMaxHealth() * 0.2F);
        });
        helper.runAfterDelay(SETTLE + 15, () -> {
            helper.assertTrue(revived.isEnraged(), "Abaixo de 30% de vida deveria se enfurecer");
            double now = revived.getAttributeValue(Attributes.ATTACK_DAMAGE);
            helper.assertTrue(Math.abs(now - damage[0] * 1.2) < 0.01 * damage[0],
                    "A furia deveria multiplicar o dano por 1,2: antes " + damage[0] + ", agora " + now);
            helper.succeed();
        });
    }
}
