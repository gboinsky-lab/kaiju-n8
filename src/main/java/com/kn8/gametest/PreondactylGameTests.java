// src/main/java/com/kn8/gametest/PreondactylGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.def.FlyerDef;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.PreondactylEntity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do Preondactyl (0.6-E): decola e sobe acima do alvo, frente blindada / costas fracas (flyer JSON) e
 * autodestruicao com a vida baixa (aviso e explosao que fere quem esta perto).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PreondactylGameTests {

    private static final String TEMPLATE = "empty_9x7x9";

    private PreondactylGameTests() {
    }

    private static PreondactylEntity spawn(GameTestHelper helper, BlockPos pos) {
        KaijuEntity kaiju = KaijuSpawner.spawn(helper.getLevel(), KN8Constants.id("preondactyl"),
                helper.absolutePos(pos)).orElseThrow();
        helper.assertTrue(kaiju instanceof PreondactylEntity, "preondactyl deveria ser o PreondactylEntity");
        return (PreondactylEntity) kaiju;
    }

    @GameTest(template = TEMPLATE, batch = "kn8_preondactyl", timeoutTicks = 120)
    public static void takesOffAndClimbsAboveTheTarget(GameTestHelper helper) {
        PreondactylEntity flyer = spawn(helper, new BlockPos(4, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        golem.setNoAi(true);
        double start = flyer.getY();
        helper.runAfterDelay(3, () -> flyer.setTarget(golem));
        // A area do teste tem teto de barreira (7 blocos): basta ver que decolou e subiu. No mundo aberto ele circula
        // a cruise_height acima do alvo (visto em jogo).
        helper.succeedWhen(() -> {
            helper.assertTrue(flyer.isFlying(), "Com alvo, o Preondactyl deveria voar");
            helper.assertTrue(flyer.getY() > start + 1.0, "Deveria subir: " + start + " -> " + flyer.getY());
            flyer.discard();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_preondactyl")
    public static void frontIsArmoredBackIsWeak(GameTestHelper helper) {
        PreondactylEntity flyer = spawn(helper, new BlockPos(4, 1, 4));
        flyer.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            flyer.setYBodyRot(0.0F);
            FlyerDef def = flyer.flyer().orElseThrow();
            Vec3 front = flyer.position().add(0, 0, 3);
            Vec3 back = flyer.position().add(0, 0, -3);
            helper.assertTrue(flyer.facingMultiplier(def, front) == def.frontDamageMultiplier(),
                    "Golpe pela frente: x" + def.frontDamageMultiplier());
            helper.assertTrue(flyer.facingMultiplier(def, back) == def.backDamageMultiplier(),
                    "Golpe pelas costas: x" + def.backDamageMultiplier());
            flyer.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = "kn8_preondactyl", timeoutTicks = 140)
    public static void selfDestructsAtLowHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // A explosao tambem quebraria blocos (fila de destruicao que continua depois do teste).
        GameRules.BooleanValue griefing = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean before = griefing.get();
        griefing.set(false, level.getServer());
        PreondactylEntity flyer = spawn(helper, new BlockPos(4, 1, 4));
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 1, 4));
        golem.setNoAi(true);
        float[] golemBefore = new float[1];
        helper.runAfterDelay(3, () -> {
            golemBefore[0] = golem.getHealth();
            flyer.setHealth(flyer.getMaxHealth() * 0.1F);
        });
        helper.runAfterDelay(8, () -> helper.assertTrue(flyer.isSelfDestructing(),
                "Com 10% da vida deveria comecar a autodestruicao"));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(!flyer.isAlive(), "Depois do aviso o Preondactyl explode");
            helper.assertTrue(golem.getHealth() < golemBefore[0], "A explosao deveria ferir o golem ao lado");
            griefing.set(before, level.getServer());
            helper.succeed();
        });
    }
}
