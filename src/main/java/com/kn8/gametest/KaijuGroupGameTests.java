// src/main/java/com/kn8/gametest/KaijuGroupGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.destruction.DestructionService;
import com.kn8.common.kaiju.CarcassEntity;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da PM-4: capacidades de terreno por especie (forca propria, aranha que contorna em vez de quebrar) e
 * papeis em grupo (o lider passa o alvo; com o lider morto, os outros reagem pelo perfil).
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuGroupGameTests {

    private static final String TEMPLATE = "empty_15x12x15";
    private static final int SETTLE = 2;

    private KaijuGroupGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void terrainCapabilitiesComeFromTheSpecies(GameTestHelper helper) {
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(3, 1, 3));
        honju.setNoAi(true);
        KaijuEntity yoju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(11, 1, 3));
        yoju.setNoAi(true);
        KaijuEntity spider = helper.spawn(KN8Entities.TRICHONEPHILA.get(), new BlockPos(7, 1, 11));
        spider.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(DestructionService.walkPower(honju) == 4, "Honju com forca propria 4, veio "
                    + DestructionService.walkPower(honju));
            helper.assertTrue(DestructionService.walkPower(yoju) == 3, "Primigenius com a forca da categoria (3)");
            helper.assertTrue(yoju.mayBreakTerrain(), "Primigenius quebra o que barra o caminho");
            helper.assertTrue(!spider.mayBreakTerrain(), "A aranha contorna (so quebra travada)");
            honju.discard();
            yoju.discard();
            spider.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60, batch = "kn8_group_leader")
    public static void leaderSharesTargetAndItsDeathChangesTheGroup(GameTestHelper helper) {
        KaijuEntity leader = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(7, 1, 3));
        leader.setNoAi(true);
        KaijuEntity calm = helper.spawn(KN8Entities.PRIMIGENIUS.get(), new BlockPos(3, 1, 9));
        calm.setNoAi(true);
        KaijuEntity revived = helper.spawn(KN8Entities.PRIMIGENIUS_RESURRECTED.get(), new BlockPos(11, 1, 9));
        revived.setNoAi(true);
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(7, 1, 13));
        golem.setNoAi(true);
        helper.runAfterDelay(SETTLE, () -> leader.setTarget(golem));
        helper.runAfterDelay(SETTLE + 12, () -> {
            helper.assertTrue(calm.getTarget() == golem && revived.getTarget() == golem,
                    "Os dois sem alvo deveriam receber o alvo do lider");
            leader.kill();
        });
        helper.runAfterDelay(SETTLE + 16, () -> {
            helper.assertTrue(calm.scattering(), "Primigenius (leader_loss retreat) deveria se afastar");
            helper.assertTrue(revived.hasEffect(MobEffects.DAMAGE_BOOST), "Ressurgido (leader_loss rage) com forca");
            helper.assertTrue(!revived.scattering(), "O ressurgido nao recua");
            calm.discard();
            revived.discard();
            golem.discard();
            helper.getLevel().getEntitiesOfClass(CarcassEntity.class,
                    golem.getBoundingBox().inflate(16)).forEach(c -> c.discard());
            helper.succeed();
        });
    }
}
