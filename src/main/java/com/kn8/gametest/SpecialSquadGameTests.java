// src/main/java/com/kn8/gametest/SpecialSquadGameTests.java
package com.kn8.gametest;

import java.util.function.Supplier;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.SpecialSoldierDef;
import com.kn8.common.registry.KN8Attachments;
import com.kn8.common.registry.KN8Entities;
import com.kn8.common.registry.KN8Items;
import com.kn8.common.soldier.special.HoshinaEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.7-C: Reno, Mina e Narumi carregam o perfil (arma, vida, aura e Release do JSON); o canhao da Mina
 * explode e fere, a municao congelante do Reno deixa lento e a estocada do Narumi acerta no tick do JSON.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialSquadGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final String LONG_TEMPLATE = "empty_9x7x33";
    private static final int SETTLE = 3;

    private SpecialSquadGameTests() {
    }

    private static <T extends HoshinaEntity> T spawn(GameTestHelper helper, EntityType<T> type, BlockPos pos) {
        T soldier = helper.spawn(type, pos);
        soldier.setNoAi(true);
        return soldier;
    }

    private static void profile(GameTestHelper helper, HoshinaEntity soldier, Supplier<Item> weapon, String aura) {
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(soldier.getMainHandItem().is(weapon.get()), "Arma do perfil na mao direita: "
                    + soldier.getMainHandItem());
            SpecialSoldierDef def = soldier.profile().orElseThrow();
            helper.assertTrue(soldier.getAttributeValue(Attributes.MAX_HEALTH) == def.health(),
                    "Vida do perfil (" + def.health() + "): " + soldier.getAttributeValue(Attributes.MAX_HEALTH));
            helper.assertTrue(soldier.getData(KN8Attachments.AURA).equals(KN8Constants.id(aura)),
                    "Aura " + aura + ": " + soldier.getData(KN8Attachments.AURA));
            helper.assertTrue(soldier.getData(KN8Attachments.RELEASE_VISUAL) == def.release(),
                    "Release do perfil: " + soldier.getData(KN8Attachments.RELEASE_VISUAL));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void renoProfileGivesTheRifle(GameTestHelper helper) {
        profile(helper, spawn(helper, KN8Entities.RENO.get(), new BlockPos(4, 1, 4)), KN8Items.RIFLE::get,
                "reno_frost");
    }

    @GameTest(template = TEMPLATE)
    public static void minaProfileGivesTheCannon(GameTestHelper helper) {
        profile(helper, spawn(helper, KN8Entities.MINA.get(), new BlockPos(4, 1, 4)), KN8Items.MINA_CANNON::get,
                "mina_command");
    }

    @GameTest(template = TEMPLATE)
    public static void narumiProfileGivesTheBayonet(GameTestHelper helper) {
        profile(helper, spawn(helper, KN8Entities.NARUMI.get(), new BlockPos(4, 1, 4)),
                KN8Items.NARUMI_BAYONET::get, "narumi_pink");
    }

    /** Tiro do canhao: explode no golem a 14 blocos (dano). */
    @GameTest(template = LONG_TEMPLATE, timeoutTicks = 120)
    public static void minaCannonShotExplodesOnTheTarget(GameTestHelper helper) {
        HoshinaEntity mina = spawn(helper, KN8Entities.MINA.get(), new BlockPos(4, 1, 2));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 16));
        golem.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            start[0] = golem.getHealth();
            mina.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(mina.startTechnique("cannon_shot", golem), "Tiro do canhao comecou");
            helper.succeedWhen(() -> helper.assertTrue(golem.getHealth() < start[0],
                    "O tiro do canhao deveria acertar o golem: " + golem.getHealth()));
        });
    }

    /** Municao congelante: o alvo fica lento. */
    @GameTest(template = LONG_TEMPLATE, timeoutTicks = 120)
    public static void renoFreezeRoundSlowsTheTarget(GameTestHelper helper) {
        HoshinaEntity reno = spawn(helper, KN8Entities.RENO.get(), new BlockPos(4, 1, 2));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 1, 14));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            reno.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(reno.startTechnique("freeze_round", golem), "Municao congelante comecou");
            helper.succeedWhen(() -> helper.assertTrue(golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "A municao congelante deveria deixar o golem lento"));
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void narumiThrustComboHits(GameTestHelper helper) {
        HoshinaEntity narumi = spawn(helper, KN8Entities.NARUMI.get(), new BlockPos(3, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(5, 1, 4));
        golem.setNoAi(true);
        float[] start = new float[1];
        helper.runAfterDelay(SETTLE, () -> {
            start[0] = golem.getHealth();
            narumi.lookAt(golem, 360.0F, 90.0F);
            helper.assertTrue(narumi.startTechnique("thrust_combo", golem), "Estocadas comecaram");
        });
        helper.runAfterDelay(SETTLE + 40, () -> {
            helper.assertTrue(golem.getHealth() < start[0], "O golem levou as estocadas: " + golem.getHealth());
            helper.succeed();
        });
    }
}
