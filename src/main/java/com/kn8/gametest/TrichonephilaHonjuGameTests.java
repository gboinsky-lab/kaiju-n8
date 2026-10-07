// src/main/java/com/kn8/gametest/TrichonephilaHonjuGameTests.java
package com.kn8.gametest;

import com.kn8.KN8Constants;
import com.kn8.common.data.KN8Data;
import com.kn8.common.data.def.KaijuClass;
import com.kn8.common.kaiju.KaijuEntity;
import com.kn8.common.kaiju.KaijuPart;
import com.kn8.common.registry.KN8Entities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests da 0.6-B: a Trichonephila Honju carrega como Honju (hitbox e partes do JSON, nucleo marcado) e o chefe
 * dela invoca a propria especie (Trichonephila), como decidido pelo Miguel.
 */
@GameTestHolder(KN8Constants.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrichonephilaHonjuGameTests {

    private TrichonephilaHonjuGameTests() {
    }

    @GameTest(template = "empty_9x7x9")
    public static void honjuLoadsWithItsPartsAndCore(GameTestHelper helper) {
        KaijuEntity queen = helper.spawn(KN8Entities.TRICHONEPHILA_HONJU.get(), new BlockPos(4, 1, 4));
        queen.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(queen.def().map(def -> def.kaijuClass() == KaijuClass.HONJU).orElse(false),
                    "A Trichonephila Honju deveria ser da categoria Honju");
            helper.assertTrue(Math.abs(queen.getBbWidth() - 6.0F) < 0.01F, "Hitbox deveria ter 6 de largura, veio "
                    + queen.getBbWidth());
            boolean core = false;
            for (KaijuPart part : queen.kaijuParts()) {
                core |= part.isCore();
            }
            helper.assertTrue(queen.kaijuParts().length == 4 && core, "Deveria ter 4 partes com nucleo");
            helper.assertTrue(KN8Data.BOSS.get(KN8Constants.id("trichonephila_honju"), false)
                    .map(boss -> boss.summon().map(summon -> summon.species().equals(KN8Constants.id("trichonephila")))
                            .orElse(false)).orElse(false),
                    "O chefe deveria invocar Trichonephila (a propria especie)");
            queen.discard();
            helper.succeed();
        });
    }
}
