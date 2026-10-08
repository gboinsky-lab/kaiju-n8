// src/main/java/com/kn8/gametest/KaijuGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.registry.KN8Entities;
import com.kn8.core.kaiju.KaijuState;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests do M7a: a entidade base aplica o JSON (curva de fortitude + multiplicadores padrao do config) e usa a
 * hitbox do JSON. Template 9x7x9 porque o Primigenius tem 4,6 blocos de altura.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KaijuGameTests {

    private static final String TEMPLATE = "empty_9x7x9";
    /** Honju (9 blocos de altura) precisa de um template maior. */
    private static final String LARGE_TEMPLATE = "empty_15x12x15";
    private static final BlockPos LARGE_CENTER = new BlockPos(7, 1, 7);
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);
    private static final int SETTLE_TICKS = 2;
    private static final double HEALTH_TOLERANCE = 0.5;
    private static final double TOLERANCE = 0.05;

    private KaijuGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void primigeniusUsesJsonBalanceAndHitbox(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS.get(), CENTER);
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            // Balanceamento v1.0 (overrides do JSON): vida 220, dano 10, armadura 11 (multiplicadores 1,0).
            helper.assertTrue(Math.abs(kaiju.getMaxHealth() - 220.0) < HEALTH_TOLERANCE,
                    "Vida maxima " + kaiju.getMaxHealth() + " diferente do JSON (220)");
            helper.assertTrue(Math.abs(kaiju.getHealth() - kaiju.getMaxHealth()) < HEALTH_TOLERANCE,
                    "Kaiju recem-invocado deveria estar com vida cheia");
            helper.assertTrue(Math.abs(kaiju.getAttributeValue(Attributes.ATTACK_DAMAGE) - 10.0) < TOLERANCE,
                    "Dano diferente do JSON (10)");
            helper.assertTrue(Math.abs(kaiju.getAttributeValue(Attributes.ARMOR) - 11.0) < TOLERANCE,
                    "Armadura diferente do JSON (11)");
            helper.assertTrue(Math.abs(kaiju.getBbWidth() - 3.13) < TOLERANCE
                    && Math.abs(kaiju.getBbHeight() - 6.0) < TOLERANCE, "Hitbox diferente do JSON (3,13 x 6,0)");
            helper.assertTrue(kaiju.state() == KaijuState.IDLE, "Sem IA e sem alvo deveria ficar IDLE");
            kaiju.discard();
            helper.succeed();
        });
    }

    /** 0.1-B: as duas especies novas existem e usam a escala nova (Yoju 6, Honju 9). */
    @GameTest(template = LARGE_TEMPLATE)
    public static void newPrimigeniusSpeciesUseTheNewScale(GameTestHelper helper) {
        KaijuEntity yoju = helper.spawn(KN8Entities.PRIMIGENIUS_RESURRECTED.get(), new BlockPos(3, 1, 7));
        KaijuEntity honju = helper.spawn(KN8Entities.PRIMIGENIUS_HONJU.get(), new BlockPos(10, 1, 7));
        yoju.setNoAi(true);
        honju.setNoAi(true);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(Math.abs(yoju.getBbHeight() - 6.0) < TOLERANCE,
                    "Yoju ressurgido deveria ter 6 de altura");
            helper.assertTrue(Math.abs(honju.getBbHeight() - 9.0) < TOLERANCE, "Honju deveria ter 9 de altura");
            helper.assertTrue(honju.getMaxHealth() > yoju.getMaxHealth(), "Honju deveria ter mais vida que o Yoju");
            yoju.discard();
            honju.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void trichonephilaIsSmallAndWeak(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.TRICHONEPHILA.get(), CENTER);
        kaiju.setNoAi(true);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            // Balanceamento v1.0 (era 56,6 pela curva da fortitude 3,5): vida 65.
            helper.assertTrue(Math.abs(kaiju.getMaxHealth() - 65.0) < HEALTH_TOLERANCE,
                    "Vida maxima " + kaiju.getMaxHealth() + " diferente do JSON (65)");
            // Modelo do Meshy de 2026-10-06: 5,5 de envergadura, corpo baixo (1,7 de altura).
            helper.assertTrue(Math.abs(kaiju.getBbWidth() - 3.4) < TOLERANCE
                    && Math.abs(kaiju.getBbHeight() - 1.8) < TOLERANCE, "Hitbox diferente do JSON (3,4 x 1,8)");
            helper.assertTrue(kaiju.kaijuId().equals(KN8Constants.id("trichonephila")),
                    "Id da especie deveria ser o do EntityType");
            kaiju.discard();
            helper.succeed();
        });
    }

    @GameTest(template = LARGE_TEMPLATE)
    public static void staggerChangesState(GameTestHelper helper) {
        KaijuEntity kaiju = helper.spawn(KN8Entities.PRIMIGENIUS_REVIVED.get(), LARGE_CENTER);
        kaiju.setNoAi(true);
        kaiju.stagger(40);
        helper.runAfterDelay(SETTLE_TICKS, () -> {
            helper.assertTrue(kaiju.state() == KaijuState.STAGGER, "Atordoado deveria estar em STAGGER");
            kaiju.discard();
            helper.succeed();
        });
    }
}
