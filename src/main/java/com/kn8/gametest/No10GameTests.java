// src/main/java/com/kn8/gametest/No10GameTests.java
package com.kn8.gametest;

import java.util.List;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.numbered.KaijuNo10Entity;
import com.kn8.common.world.KaijuSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do Kaiju No. 10 (0.6-E): as duas formas existem com a vida da fortitude (acima do teto vanilla de 1024
 * na gigante) e a forma pequena vira a gigante com a vida abaixo de 50% ({@code transform} do numbered/*.json).
 * Lote proprio: a forma gigante tem 24 m e quebra blocos em volta ao surgir.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class No10GameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    private static final double AREA = 40.0;

    private No10GameTests() {
    }

    @GameTest(template = TEMPLATE, batch = "kn8_no10", timeoutTicks = 100)
    public static void smallFormBecomesGiantAtHalfHealth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        KaijuEntity small = KaijuSpawner.spawn(level, KN8Constants.id("kaiju_no10_small"), center).orElseThrow();
        helper.assertTrue(small instanceof KaijuNo10Entity, "kaiju_no10_small deveria ser o KaijuNo10Entity");
        small.setNoAi(true);
        // A forma gigante quebra blocos em volta ao surgir; a fila de destruicao continuava depois do teste e
        // atrapalhava os lotes seguintes (ressurreicao em massa). Aqui so importa a troca de forma.
        GameRules.BooleanValue griefing = level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean griefingBefore = griefing.get();
        griefing.set(false, level.getServer());
        AABB area = new AABB(center).inflate(AREA);
        helper.runAfterDelay(5, () -> {
            // 20 x 2^(8,3 - 2) = 1576 de vida (acima de 1024: AttributeLimits).
            helper.assertTrue(small.getMaxHealth() > 1500.0F, "Vida da forma pequena: " + small.getMaxHealth());
            small.setHealth(small.getMaxHealth() * 0.4F);
        });
        helper.runAfterDelay(15, () -> {
            List<KaijuEntity> giants = level.getEntitiesOfClass(KaijuEntity.class, area,
                    kaiju -> kaiju.kaijuId().equals(KN8Constants.id("kaiju_no10_giant")));
            helper.assertTrue(small.isRemoved(), "A forma pequena deveria sumir");
            helper.assertTrue(giants.size() == 1, "Deveria surgir 1 forma gigante, vieram " + giants.size());
            KaijuEntity giant = giants.get(0);
            helper.assertTrue(giant.getMaxHealth() > 2500.0F, "Vida da forma gigante: " + giant.getMaxHealth());
            helper.assertTrue(giant.getHealth() == giant.getMaxHealth(), "A forma gigante nasce com a vida cheia");
            level.getEntitiesOfClass(Entity.class, area, entity -> entity instanceof KaijuEntity)
                    .forEach(Entity::discard);
            griefing.set(griefingBefore, level.getServer());
            helper.succeed();
        });
    }
}
